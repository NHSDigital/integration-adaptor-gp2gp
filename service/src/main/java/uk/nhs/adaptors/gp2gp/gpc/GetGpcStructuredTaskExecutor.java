package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.common.utils.BinaryUtils.getBytesLengthOfString;
import static uk.nhs.adaptors.gp2gp.common.utils.Gzip.compress;
import static uk.nhs.adaptors.gp2gp.ehr.utils.AbsentAttachmentUtils.buildAbsentAttachmentFileName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.GetAbsentAttachmentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    public static final String SKELETON_ATTACHMENT = "X-GP2GP-Skeleton: Yes";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String XML_CONTENT_TYPE = "application/xml";
    public static final String TEXT_XML_CONTENT_TYPE = "text/xml";
    private static final String LEADING_UNDERSCORE_CHAR = "_";

    private final TimestampService timestampService;
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
    private final EhrDocumentMapper ehrDocumentMapper;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        List<OutboundMessage.ExternalAttachment> externalAttachments = new ArrayList<>();
        List<OutboundMessage.ExternalAttachment> absentAttachments = new ArrayList<>();
        List<EhrExtractStatus.GpcDocument> ehrStatusGpcDocuments = new ArrayList<>();
        var now = this.timestampService.now();
        var bundle = getStructuredRecord(structuredTaskDefinition);

        try {
            var structuredRecordExternalAttachments = structuredRecordMappingService.getExternalAttachments(bundle);
            var documentsAsExternalAttachments = getDocumentsAsExternalAttachments(structuredRecordExternalAttachments);

            populateExternalAttachments(
                    structuredTaskDefinition,
                    documentsAsExternalAttachments,
                    ehrStatusGpcDocuments,
                    externalAttachments,
                    now);

            var structuredRecordAbsentAttachments = structuredRecordMappingService.getAbsentAttachments(bundle);

            populateAbsentAttachments(
                    structuredRecordExternalAttachments,
                    structuredRecordAbsentAttachments,
                    absentAttachments);

            populateEhrStatusGpcDocuments(
                    structuredTaskDefinition,
                    absentAttachments,
                    ehrStatusGpcDocuments,
                    now);

            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(
                structuredTaskDefinition, ehrStatusGpcDocuments
            );

            queueGetDocumentTasks(structuredTaskDefinition, documentsAsExternalAttachments);
            queueGetAbsentAttachmentTasks(structuredTaskDefinition, absentAttachments);

        } finally {
            messageContext.resetMessageContext();
        }

        var ehrExtractXml = generateEhrExtractXml(
                structuredTaskDefinition,
                bundle,
                externalAttachments,
                ehrStatusGpcDocuments,
                now);

        var storageDataWrapper = getStorageDataWrapper(
                structuredTaskDefinition,
                externalAttachments,
                absentAttachments,
                ehrExtractXml);

        var structuredRecordJsonFilename = GpcFilenameUtils
                .generateStructuredRecordFilename(structuredTaskDefinition.getConversationId());

        storageConnectorService.uploadFile(storageDataWrapper, structuredRecordJsonFilename);

        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(
            structuredTaskDefinition,
            structuredRecordJsonFilename
        );

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private StorageDataWrapper getStorageDataWrapper(
            GetGpcStructuredTaskDefinition structuredTaskDefinition,
            List<OutboundMessage.ExternalAttachment> externalAttachments,
            List<OutboundMessage.ExternalAttachment> absentAttachments,
            String ehrExtractXml) throws JsonProcessingException {

        var stringRequestBody = getStringRequestBodyWithAllExternalAttachments(
                externalAttachments,
                absentAttachments,
                ehrExtractXml);

        return StorageDataWrapperProvider.buildStorageDataWrapper(
                structuredTaskDefinition,
                stringRequestBody,
                structuredTaskDefinition.getTaskId());
    }

    private String getStringRequestBodyWithAllExternalAttachments(
            List<OutboundMessage.ExternalAttachment> externalAttachments,
            List<OutboundMessage.ExternalAttachment> absentAttachments,
            String ehrExtractXml) throws JsonProcessingException {

        var allExternalAttachments = Stream
            .concat(externalAttachments.stream(), absentAttachments.stream())
            .collect(Collectors.toList());

        return objectMapper.writeValueAsString(OutboundMessage.builder()
            .payload(ehrExtractXml)
            .attachments(Collections.emptyList())
            .externalAttachments(mapPrefixesToDocumentIds(allExternalAttachments))
            .build()
        );
    }

    private static void populateEhrStatusGpcDocuments(
            GetGpcStructuredTaskDefinition structuredTaskDefinition,
            List<OutboundMessage.ExternalAttachment> absentAttachments,
            List<EhrExtractStatus.GpcDocument> ehrStatusGpcDocuments,
            Instant now) {

        absentAttachments.stream()
            .map(absentAttachment -> EhrExtractStatus.GpcDocument.builder()
                .documentId(absentAttachment.getDocumentId())
                .fileName(buildAbsentAttachmentFileName(absentAttachment.getDocumentId()))
                .contentType(StructuredRecordMappingService.DEFAULT_ATTACHMENT_CONTENT_TYPE)
                .accessDocumentUrl(null)
                .objectName(null)
                .accessedAt(now)
                .taskId(structuredTaskDefinition.getTaskId())
                .messageId(structuredTaskDefinition.getConversationId())
                .isSkeleton(false)
                .identifier(absentAttachment.getIdentifier())
                .originalDescription(absentAttachment.getOriginalDescription())
                .build())
            .forEach(ehrStatusGpcDocuments::add);
    }

    private static void populateAbsentAttachments(
            List<OutboundMessage.ExternalAttachment> structuredRecordExternalAttachments,
            List<OutboundMessage.ExternalAttachment> structuredRecordAbsentAttachments,
            List<OutboundMessage.ExternalAttachment> absentAttachments) {

        structuredRecordExternalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isBlank(documentMetadata.getUrl()))
                .peek(absentAttachment ->
                        LOGGER.warn("DocumentReference missing URL: {}", absentAttachment.getDocumentId()))
                .forEach(absentAttachments::add);

        absentAttachments.addAll(structuredRecordAbsentAttachments);
    }

    private static void populateExternalAttachments(
            GetGpcStructuredTaskDefinition structuredTaskDefinition,
            List<OutboundMessage.ExternalAttachment> documentsAsExternalAttachments,
            List<EhrExtractStatus.GpcDocument> ehrStatusGpcDocuments,
            List<OutboundMessage.ExternalAttachment> externalAttachments,
            Instant now) {

        documentsAsExternalAttachments.stream()
            .map(externalAttachment -> EhrExtractStatus.GpcDocument.builder()
                .documentId(externalAttachment.getDocumentId())
                .accessDocumentUrl(externalAttachment.getUrl())
                .objectName(null)
                .accessedAt(now)
                .taskId(structuredTaskDefinition.getTaskId())
                .messageId(structuredTaskDefinition.getConversationId())
                .isSkeleton(false)
                .identifier(externalAttachment.getIdentifier())
                .fileName(externalAttachment.getFilename())
                .contentType(externalAttachment.getContentType())
                .originalDescription(externalAttachment.getOriginalDescription())
                .build())
            .forEach(ehrStatusGpcDocuments::add);

        externalAttachments.addAll(documentsAsExternalAttachments);
    }

    private List<OutboundMessage.ExternalAttachment> getDocumentsAsExternalAttachments(
            List<OutboundMessage.ExternalAttachment> documentsAsExternalAttachments) {

        return documentsAsExternalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isNotBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());
    }

    private String generateEhrExtractXml(
            GetGpcStructuredTaskDefinition structuredTaskDefinition,
            Bundle structuredRecord,
            List<OutboundMessage.ExternalAttachment> externalAttachments,
            List<EhrExtractStatus.GpcDocument> ehrStatusGpcDocuments,
            Instant now) {

        messageContext.initialize(structuredRecord);

        var ehrExtractXml = structuredRecordMappingService
                .mapStructuredRecordToEhrExtractXml(structuredTaskDefinition, structuredRecord);

        if (isLargeEhrExtract(ehrExtractXml)) {
            LOGGER.info("EHR extract IS large");

            ehrExtractXml = Base64Utils.toBase64String(compress(ehrExtractXml));

            var messageId = randomIdGeneratorService.createNewId();
            var documentId = randomIdGeneratorService.createNewId();
            var fileName = GpcFilenameUtils.generateLargeExrExtractFilename(documentId);
            var compressedAndEncodedEhrExtractSize = ehrExtractXml.length();

            var largeEhrExtractXmlAsExternalAttachment = buildExternalAttachmentForLargeEhrExtract(
                    compressedAndEncodedEhrExtractSize,
                    messageId,
                    documentId,
                    fileName);

            externalAttachments.add(largeEhrExtractXmlAsExternalAttachment);

            var getDocumentTaskDefinition = buildGetDocumentTask(
                    structuredTaskDefinition,
                    largeEhrExtractXmlAsExternalAttachment);

            var ehrDocumentTemplateParameters = ehrDocumentMapper.mapToMhsPayloadTemplateParameters(
                    getDocumentTaskDefinition,
                    XML_CONTENT_TYPE);

            var mhsPayload = ehrDocumentMapper.mapMhsPayloadTemplateToXml(ehrDocumentTemplateParameters);
            uploadToStorage(getDocumentTaskDefinition, ehrExtractXml, mhsPayload, fileName);

            var GpcDocument = buildGpcDocument(getDocumentTaskDefinition, documentId, messageId, fileName, now);
            ehrStatusGpcDocuments.add(GpcDocument);

            ehrExtractXml = structuredRecordMappingService.buildSkeletonEhrExtractXml(structuredTaskDefinition, structuredRecord, documentId);
        }
        return ehrExtractXml;
    }

    private static EhrExtractStatus.GpcDocument buildGpcDocument(
            GetGpcDocumentTaskDefinition getDocumentTaskDefinition,
            String documentId,
            String messageId,
            String fileName,
            Instant now) {

        return EhrExtractStatus.GpcDocument.builder()
                .documentId(documentId)
                .accessDocumentUrl(null)
                .contentType(TEXT_XML_CONTENT_TYPE)
                .objectName(fileName)
                .fileName(fileName)
                .accessedAt(now)
                .taskId(getDocumentTaskDefinition.getTaskId())
                .messageId(messageId)
                .isSkeleton(true)
                .identifier(null)
                .build();
    }

    private Bundle getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);
    }

    private void queueGetDocumentTasks(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> externalAttachments) {
        externalAttachments.stream()
            .filter(externalAttachment -> StringUtils.isNotBlank(externalAttachment.getUrl()))
            .map(externalAttachment -> buildGetDocumentTask(taskDefinition, externalAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private void queueGetAbsentAttachmentTasks(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> absentAttachments) {
        absentAttachments.stream()
            .map(absentAttachment -> buildGetAbsentAttachmentTask(taskDefinition, absentAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private GetAbsentAttachmentTaskDefinition buildGetAbsentAttachmentTask(
        TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment absentAttachment) {

        return GetAbsentAttachmentTaskDefinition.builder()
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
            .originalDescription(absentAttachment.getOriginalDescription())
            .build();
    }

    private GetGpcDocumentTaskDefinition buildGetDocumentTask(
        TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment externalAttachment) {

        return GetGpcDocumentTaskDefinition.builder()
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .documentId(externalAttachment.getDocumentId())
            .accessDocumentUrl(externalAttachment.getUrl())
            .messageId(externalAttachment.getMessageId())
            .title(externalAttachment.getTitle())
            .originalDescription(externalAttachment.getOriginalDescription())
            .build();
    }

    private boolean isLargeEhrExtract(String ehrExtract) {
        return getBytesLengthOfString(ehrExtract) > gp2gpConfiguration.getLargeEhrExtractThreshold();
    }

    private List<OutboundMessage.ExternalAttachment> mapPrefixesToDocumentIds(
            List<OutboundMessage.ExternalAttachment> externalAttachments) {

        return externalAttachments.stream()
            .peek(externalAttachment -> externalAttachment.setDocumentId(
                    LEADING_UNDERSCORE_CHAR.concat(externalAttachment.getDocumentId())))
            .collect(Collectors.toList());
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachmentForLargeEhrExtract(
            int compressedAndEncodedEhrExtractSize,
            String messageId,
            String documentId,
            String fileName) {

        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentId)
            .messageId(messageId)
            .filename(fileName)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName(fileName)
                .contentType(TEXT_XML_CONTENT_TYPE)
                .length(compressedAndEncodedEhrExtractSize)
                .compressed(true)
                .largeAttachment(compressedAndEncodedEhrExtractSize > gp2gpConfiguration.getLargeAttachmentThreshold())
                .originalBase64(false)
                .domainData(SKELETON_ATTACHMENT)
                .build()
                .toString())
            .url(StringUtils.EMPTY)
            .build();
    }

    @SneakyThrows
    private void uploadToStorage(
            DocumentTaskDefinition taskDefinition,
            String ehrExtract,
            String mhsPayload,
            String fileName) {

        var taskId = taskDefinition.getTaskId();

        // Building outbound message for upload to storage as that is how DocumentSender
        // downloads and parses from storage
        var attachment = OutboundMessage.Attachment.builder()
                .contentType(TEXT_XML_CONTENT_TYPE)
                .isBase64(Boolean.TRUE)
                .description(taskDefinition.getDocumentId())
                .payload(ehrExtract)
                .build();

        var outboundMessage = OutboundMessage.builder()
            .payload(mhsPayload)
            .attachments(Collections.singletonList(attachment))
            .build();

        var outboundMessageString = OBJECT_MAPPER.writeValueAsString(outboundMessage);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, outboundMessageString, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, fileName);
    }
}
