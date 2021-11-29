package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.ResourceType;
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
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AbsentAttachmentFileMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.nhs.adaptors.gp2gp.common.utils.Gzip.compress;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    public static final String SKELETON_ATTACHMENT = "X-GP2GP-Skeleton: Yes";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String XML_CONTENT_TYPE = "application/xml";

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
        String hl7TranslatedResponse;
        List<OutboundMessage.Attachment> attachments = new ArrayList<>();
        List<OutboundMessage.ExternalAttachment> externalAttachments;

        var structuredRecord = getStructuredRecord(structuredTaskDefinition);

        try {
            messageContext.initialize(structuredRecord);

            externalAttachments = structuredRecordMappingService.getExternalAttachments(structuredRecord);

            //Here the change begins
            String fileContent = "";
            if(externalAttachmentsContainAbsentAttachment(externalAttachments)){
                LOGGER.debug("Found AbsentAttachments candidates");
                var documentReferences = extractDocumentReferences(structuredRecord);

                final var externalAttachmentList = externalAttachments; //lambda has to refer to final values
                var title= Optional.ofNullable(
                    documentReferences.stream()
                        .filter(documentReference -> documentReference.getContentFirstRep().getAttachment().getUrl()
                            .equals(externalAttachmentList.get(0).getUrl())
                        ).findFirst().get().getContentFirstRep().getAttachment().getTitle()
                    ).orElse(StringUtils.EMPTY);

                fileContent = AbsentAttachmentFileMapper.mapDataToAbsentAttachment(
                    title, structuredTaskDefinition.getToOdsCode(), structuredTaskDefinition.getConversationId()
                );
            }
            //Here the change ends (for now)

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

            hl7TranslatedResponse = structuredRecordMappingService.getHL7(structuredTaskDefinition, structuredRecord);

            queueGetDocumentsTask(structuredTaskDefinition, externalAttachments);
        } finally {
            messageContext.resetMessageContext();
        }

        if (isLargeEhrExtract(hl7TranslatedResponse)) {
            var compressedBytes = compress(hl7TranslatedResponse);
            if (compressedBytes == null) {
                throw new RuntimeException();
            }
            hl7TranslatedResponse = new String(compressedBytes, UTF_8);

            if (!isLargeEhrExtract(hl7TranslatedResponse)) {
                var filename = GpcFilenameUtils.generateDocumentFilename(
                    structuredTaskDefinition.getConversationId(), randomIdGeneratorService.createNewId()
                );
                var attachment = buildAttachment(hl7TranslatedResponse, filename);
                attachments.add(attachment);
                hl7TranslatedResponse = structuredRecordMappingService.getHL7ForLargeEhrExtract(structuredTaskDefinition, filename);
            } else {
                var documentId = randomIdGeneratorService.createNewId();
                var documentName = GpcFilenameUtils.generateDocumentFilename(
                    structuredTaskDefinition.getConversationId(), documentId
                );
                var externalAttachment = buildExternalAttachment(hl7TranslatedResponse, structuredTaskDefinition, documentId, documentName);
                externalAttachments.add(externalAttachment);

                var taskDefinition = buildGetDocumentTask(structuredTaskDefinition, externalAttachment);
                uploadToStorage(hl7TranslatedResponse, documentName, taskDefinition);
                ehrExtractStatusService.updateEhrExtractStatusWithEhrExtractChunks(structuredTaskDefinition, externalAttachment);

                hl7TranslatedResponse = structuredRecordMappingService.getHL7ForLargeEhrExtract(structuredTaskDefinition,
                    externalAttachment.getFilename());
            }
        }

        var outboundMessage = OutboundMessage.builder()
            .payload(hl7TranslatedResponse)
            .attachments(attachments)
            .externalAttachments(externalAttachments)
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

    private List<DocumentReference> extractDocumentReferences(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(e -> e.getResourceType().equals(ResourceType.DocumentReference))
            .map(DocumentReference.class::cast).collect(Collectors.toList());
    }

    private boolean externalAttachmentsContainAbsentAttachment(List<OutboundMessage.ExternalAttachment> externalAttachments) {
        return externalAttachments.stream().anyMatch(e -> e.getFilename().contains("AbsentAttachment"));
    }

    private Bundle getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);
    }

    private void queueGetDocumentsTask(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> externalAttachments) {
        externalAttachments.stream()
            .map(externalAttachment -> buildGetDocumentTask(taskDefinition, externalAttachment))
            .forEach(taskDispatcher::createTask);
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

    private OutboundMessage.Attachment buildAttachment(String content, String filename) {
        return OutboundMessage.Attachment.builder()
            .payload(content)
            .contentType(XML_CONTENT_TYPE)
            .isBase64(Boolean.FALSE.toString())
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
                .isBase64(Boolean.FALSE.toString())
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

    private int getBytesLengthOfString(String input) {
        return input.getBytes(UTF_8).length;
    }
}
