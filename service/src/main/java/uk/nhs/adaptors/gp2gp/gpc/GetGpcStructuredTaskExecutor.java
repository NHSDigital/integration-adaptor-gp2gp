package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.SendAbsentAttachmentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

import static uk.nhs.adaptors.gp2gp.common.utils.BinaryUtils.getBytesLengthOfString;
import static uk.nhs.adaptors.gp2gp.common.utils.Gzip.compress;
import static uk.nhs.adaptors.gp2gp.ehr.utils.AbsentAttachmentUtils.buildAbsentAttachmentFileName;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    public static final String SKELETON_ATTACHMENT = "X-GP2GP-Skeleton: Yes";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String XML_CONTENT_TYPE = "application/xml";
    private static final String LEADING_UNDERSCORE_CHAR = "_";

    private final GpcClient gpcClient;
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final MessageContext messageContext;
    private final FhirParseService fhirParseService;
    private final ObjectMapper objectMapper;
    private final StructuredRecordMappingService structuredRecordMappingService;
    private final TaskDispatcher taskDispatcher;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final RandomIdGeneratorService randomIdGeneratorService;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        String ehrExtract;
        List<OutboundMessage.Attachment> attachments = new ArrayList<>();
        List<OutboundMessage.ExternalAttachment> externalAttachments;
        List<OutboundMessage.ExternalAttachment> absentAttachments;

        var structuredRecord = getStructuredRecord(structuredTaskDefinition);

        try {
            messageContext.initialize(structuredRecord);

            externalAttachments = structuredRecordMappingService.getExternalAttachments(structuredRecord);
            absentAttachments = structuredRecordMappingService.getAbsentAttachments(structuredRecord);

            var documentReferencesWithoutUrl = externalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());
            if (!documentReferencesWithoutUrl.isEmpty()) {
                LOGGER.warn("Following DocumentReference resources does not have any Attachments with URL: {}",
                    documentReferencesWithoutUrl);
            }

            externalAttachments = externalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isNotBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());

            var urls = externalAttachments.stream()
                .collect(Collectors.toMap(OutboundMessage.ExternalAttachment::getDocumentId, OutboundMessage.ExternalAttachment::getUrl));
            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(structuredTaskDefinition, urls);

            var absentAttachmentFileNames = absentAttachments.stream()
                .collect(Collectors.toMap(
                    OutboundMessage.ExternalAttachment::getDocumentId,
                    absentAttachment -> buildAbsentAttachmentFileName(
                        structuredTaskDefinition.getConversationId(), absentAttachment.getDocumentId()
                    ))
                );

            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferencesAbsent(
                structuredTaskDefinition, absentAttachmentFileNames
            );

            ehrExtract = structuredRecordMappingService.getHL7(structuredTaskDefinition, structuredRecord);

            queueGetDocumentsTask(structuredTaskDefinition, externalAttachments);
            queueSendAbsentAttachmentTask(structuredTaskDefinition, absentAttachments);
        } finally {
            messageContext.resetMessageContext();
        }

        LOGGER.info("Checking EHR Extract size");
        if (isLargeEhrExtract(ehrExtract)) {
            LOGGER.info("EHR extract IS large");
            ehrExtract = Base64Utils.toBase64String(compress(ehrExtract));

            LOGGER.info("Checking Compressed EHR Extract size");
            if (isLargeEhrExtract(ehrExtract)) {
                LOGGER.info("Compressed EHR extract IS large");
                var documentId = randomIdGeneratorService.createNewId();
                var documentName = GpcFilenameUtils.generateDocumentFilename(
                    structuredTaskDefinition.getConversationId(), documentId
                );
                var externalAttachment = buildExternalAttachment(ehrExtract, structuredTaskDefinition, documentId, documentName);
                externalAttachments.add(externalAttachment);

                var taskDefinition = buildGetDocumentTask(structuredTaskDefinition, externalAttachment);
                uploadToStorage(ehrExtract, documentName, taskDefinition);
                ehrExtractStatusService.updateEhrExtractStatusWithEhrExtractChunks(structuredTaskDefinition, externalAttachment);

                ehrExtract = structuredRecordMappingService.getHL7ForLargeEhrExtract(
                    structuredTaskDefinition, externalAttachment.getFilename());
            } else {
                LOGGER.info("Compressed EHR extract IS NOT large");
                var filename = GpcFilenameUtils.generateDocumentFilename(
                    structuredTaskDefinition.getConversationId(), randomIdGeneratorService.createNewId()
                );
                var attachment = buildAttachment(ehrExtract, filename);
                attachments.add(attachment);
                ehrExtract = structuredRecordMappingService.getHL7ForLargeEhrExtract(structuredTaskDefinition, filename);
            }
        }

        var allExternalAttachments = Stream
            .concat(externalAttachments.stream(), absentAttachments.stream())
            .collect(Collectors.toList());

        var outboundMessage = OutboundMessage.builder()
            .payload(ehrExtract)
            .attachments(attachments)
            .externalAttachments(mapPrefixesToDocumentIds(allExternalAttachments))
            .build();

        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);
        StorageDataWrapper storageDataWrapper = StorageDataWrapperProvider.buildStorageDataWrapper(
            structuredTaskDefinition,
            stringRequestBody,
            structuredTaskDefinition.getTaskId()
        );

        String structuredRecordJsonFilename = GpcFilenameUtils.generateStructuredRecordFilename(
            structuredTaskDefinition.getConversationId()
        );

        storageConnectorService.uploadFile(storageDataWrapper, structuredRecordJsonFilename);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(
            structuredTaskDefinition,
            structuredRecordJsonFilename
        );

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private Bundle getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);
    }

    private void queueGetDocumentsTask(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> externalAttachments) {
        externalAttachments.stream()
            .map(externalAttachment -> buildGetDocumentTask(taskDefinition, externalAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private void queueSendAbsentAttachmentTask(TaskDefinition taskDefinition,
        List<OutboundMessage.ExternalAttachment> absentAttachments) {
        absentAttachments.stream()
            .map(absentAttachment -> buildSendAbsentAttachmentTask(taskDefinition, absentAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private SendAbsentAttachmentTaskDefinition buildSendAbsentAttachmentTask(TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment absentAttachment) {
        return SendAbsentAttachmentTaskDefinition.builder()
            .documentId(absentAttachment.getDocumentId())
            .title(absentAttachment.getTitle())
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .messageId(absentAttachment.getMessageId())
            .build();
    }

    private GetGpcDocumentTaskDefinition buildGetDocumentTask(TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment externalAttachment) {
        return GetGpcDocumentTaskDefinition.builder()
            .documentId(externalAttachment.getDocumentId())
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .accessDocumentUrl(externalAttachment.getUrl())
            .messageId(externalAttachment.getMessageId())
            .build();
    }

    private boolean isLargeEhrExtract(String ehrExtract) {
        return getBytesLengthOfString(ehrExtract) > gp2gpConfiguration.getLargeEhrExtractThreshold();
    }

    private List<OutboundMessage.ExternalAttachment> mapPrefixesToDocumentIds(List<OutboundMessage.ExternalAttachment> extAttachments) {
        return extAttachments.stream()
            .peek(externalAttachment -> externalAttachment.setDocumentId(
                LEADING_UNDERSCORE_CHAR.concat(externalAttachment.getDocumentId())
            ))
            .collect(Collectors.toList());
    }

    private OutboundMessage.Attachment buildAttachment(String content, String filename) {
        return OutboundMessage.Attachment.builder()
            .payload(content)
            .contentType(XML_CONTENT_TYPE)
            .isBase64(Boolean.FALSE)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName(filename)
                .contentType(XML_CONTENT_TYPE)
                .compressed(true) // always compressed at this stage
                .largeAttachment(true)
                .originalBase64(false)
                .length(getBytesLengthOfString(content))
                .domainData(SKELETON_ATTACHMENT)
                .build()
                .toString()
            )
            .build();
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachment(String ehrExtract, TaskDefinition taskDefinition,
        String documentId, String documentName) {
        var messageId = randomIdGeneratorService.createNewId();

        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentId)
            .messageId(messageId)
            .filename(documentName)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName(documentName)
                .contentType(XML_CONTENT_TYPE) // confirm this is correct
                .length(getBytesLengthOfString(ehrExtract))
                .compressed(true) // always compressed at this stage
                .largeAttachment(true)
                .originalBase64(false)
                .domainData(SKELETON_ATTACHMENT)
                .build()
                .toString())
            .url(StringUtils.EMPTY)
            .build();
    }

    @SneakyThrows
    private void uploadToStorage(String ehrExtract, String documentName, DocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();

        // Building outbound message for upload to storage as that is how DocumentSender downloads and parses from storage
        var attachments = Collections.singletonList(
            OutboundMessage.Attachment.builder()
                .contentType(XML_CONTENT_TYPE)
                .isBase64(Boolean.FALSE)
                .description(taskDefinition.getDocumentId())
                .payload(ehrExtract)
                .build());
        var outboundMessage = OutboundMessage.builder()
            .payload(StringUtils.EMPTY)
            .attachments(attachments)
            .build();

        var outboundMessageString = OBJECT_MAPPER.writeValueAsString(outboundMessage);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, outboundMessageString, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);
    }
}
