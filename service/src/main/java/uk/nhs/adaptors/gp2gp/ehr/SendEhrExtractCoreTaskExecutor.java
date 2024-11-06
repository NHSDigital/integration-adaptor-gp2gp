package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;
import uk.nhs.adaptors.gp2gp.common.utils.Gzip;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GpcFilenameUtils;
import uk.nhs.adaptors.gp2gp.gpc.StorageDataWrapperProvider;
import uk.nhs.adaptors.gp2gp.gpc.StructuredRecordMappingService;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static uk.nhs.adaptors.gp2gp.common.utils.BinaryUtils.getBytesLengthOfString;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrExtractCoreTaskExecutor implements TaskExecutor<SendEhrExtractCoreTaskDefinition> {
    public static final String SKELETON_ATTACHMENT = "X-GP2GP-Skeleton: Yes";

    public static final String TEXT_XML_CONTENT_TYPE = "text/xml";
    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final StorageConnectorService storageConnectorService;
    private final SendAcknowledgementTaskDispatcher sendAcknowledgementTaskDispatcher;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final StructuredRecordMappingService structuredRecordMappingService;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final EhrDocumentMapper ehrDocumentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Class<SendEhrExtractCoreTaskDefinition> getTaskType() {
        return SendEhrExtractCoreTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition) {
        String structuredRecordFilename = GpcFilenameUtils.generateStructuredRecordFilename(
            sendEhrExtractCoreTaskDefinition.getConversationId());
        var storageDataWrapper = storageConnectorService.downloadFile(structuredRecordFilename);

        var documentObjectNameAndSize = ehrExtractStatusService
            .fetchDocumentObjectNameAndSize(sendEhrExtractCoreTaskDefinition.getConversationId());

        String outboundEhrExtract = replacePlaceholders(documentObjectNameAndSize, storageDataWrapper.getData());

        LOGGER.info("Checking EHR Extract size");
        final var outboundMessage = objectMapper.readValue(outboundEhrExtract, OutboundMessage.class);
        if (getBytesLengthOfString(outboundMessage.getPayload()) > gp2gpConfiguration.getLargeEhrExtractThreshold()) {
            LOGGER.info("EHR extract IS large");
            outboundEhrExtract = compressEhrExtractAndReplacePayloadWithSkeleton(sendEhrExtractCoreTaskDefinition, outboundMessage);
        }

        var requestData = mhsRequestBuilder.buildSendEhrExtractCoreRequest(
                outboundEhrExtract,
                sendEhrExtractCoreTaskDefinition.getConversationId(),
                sendEhrExtractCoreTaskDefinition.getFromOdsCode(),
                sendEhrExtractCoreTaskDefinition.getEhrExtractMessageId()
        );

        mhsClient.sendMessageToMHS(requestData);

        Instant requestSentAtPending = Instant.now();
        ehrExtractStatusService.updateEhrExtractStatusCorePending(sendEhrExtractCoreTaskDefinition, requestSentAtPending);

        Instant requestSentAt = Instant.now();
        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusCore(sendEhrExtractCoreTaskDefinition, requestSentAt);
        if (ehrExtractStatus.getGpcAccessDocument().getDocuments().isEmpty()) {
            sendAcknowledgementTaskDispatcher.sendPositiveAcknowledgement(ehrExtractStatus);
        }
    }

    private String compressEhrExtractAndReplacePayloadWithSkeleton(
            SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition,
            OutboundMessage outboundMessage) throws JsonProcessingException {
        String documentId = randomIdGeneratorService.createNewId();
        String messageId = randomIdGeneratorService.createNewId();
        String taskId = randomIdGeneratorService.createNewId();
        String fileName = GpcFilenameUtils.generateLargeExrExtractFilename(documentId);
        final var compressedEhrExtract = Base64Utils.toBase64String(Gzip.compress(outboundMessage.getPayload()));
        storeCompressedEhrExtractAsDocument(sendEhrExtractCoreTaskDefinition, documentId, messageId, fileName, taskId);

        uploadCompressedEhrExtractToStorageWrapper(
                sendEhrExtractCoreTaskDefinition, messageId, documentId, compressedEhrExtract, taskId, fileName);

        outboundMessage.setPayload(structuredRecordMappingService.buildSkeletonEhrExtractXml(outboundMessage.getPayload(), documentId));
        referenceCompressedEhrExtractDocumentAsAttachmentInOutboundMessage(
                outboundMessage, documentId, messageId, fileName, compressedEhrExtract.length());

        return objectMapper.writeValueAsString(outboundMessage);
    }

    private void referenceCompressedEhrExtractDocumentAsAttachmentInOutboundMessage(
            OutboundMessage outboundMessage,
            String documentId,
            String messageId,
            String fileName,
            int compressedEhrExtractSize) {

        outboundMessage.getExternalAttachments().add(
            OutboundMessage.ExternalAttachment.builder()
                .documentId("_" + documentId)
                .messageId(messageId)
                .description(OutboundMessage.AttachmentDescription.builder()
                    .fileName(fileName)
                    .contentType(TEXT_XML_CONTENT_TYPE)
                    .length(compressedEhrExtractSize)
                    .compressed(true)
                    .largeAttachment(compressedEhrExtractSize > gp2gpConfiguration.getLargeAttachmentThreshold())
                    .originalBase64(false)
                    .domainData(SKELETON_ATTACHMENT)
                    .build()
                    .toString()
                ).build()
        );
    }

    private void uploadCompressedEhrExtractToStorageWrapper(
            SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition,
            String messageId,
            String documentId,
            String compressedEhrExtract,
            String taskId,
            String fileName) throws JsonProcessingException {

        String data = objectMapper.writeValueAsString(
            OutboundMessage.builder()
            .payload(
                ehrDocumentMapper.generateMhsPayload(
                    sendEhrExtractCoreTaskDefinition,
                    messageId,
                    documentId,
                    "application/xml"
                )
            ).attachments(
                List.of(
                    OutboundMessage.Attachment.builder()
                        .contentType(TEXT_XML_CONTENT_TYPE)
                        .isBase64(true)
                        .description(documentId)
                        .payload(compressedEhrExtract)
                        .build()
                )
            ).build()
        );

        storageConnectorService.uploadFile(
            StorageDataWrapperProvider.buildStorageDataWrapper(sendEhrExtractCoreTaskDefinition, data, taskId),
            fileName
        );
    }

    private void storeCompressedEhrExtractAsDocument(
        SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition,
        String documentId,
        String messageId,
        String fileName,
        String taskId) {

        ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(
            sendEhrExtractCoreTaskDefinition.getConversationId(), List.of(
                EhrExtractStatus.GpcDocument.builder()
                    .documentId(documentId)
                    .messageId(messageId)
                    .objectName(fileName)
                    .fileName(fileName)
                    .accessedAt(timestampService.now())
                    .taskId(taskId)
                    .contentType(TEXT_XML_CONTENT_TYPE)
                    .isSkeleton(true)
                    .build()));
    }

    private String replacePlaceholders(Map<String, String> replacements, String data) {
        StringSubstitutor sub = new StringSubstitutor(replacements);

        return sub.replace(data);
    }
}
