package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GpcFilenameUtils;
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
    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final StorageConnectorService storageConnectorService;
    private final SendAcknowledgementTaskDispatcher sendAcknowledgementTaskDispatcher;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final StructuredRecordMappingService structuredRecordMappingService;

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
        final var outboundMessage = new ObjectMapper().readValue(outboundEhrExtract, OutboundMessage.class);

        if (getBytesLengthOfString(outboundMessage.getPayload()) > gp2gpConfiguration.getLargeEhrExtractThreshold()) {
            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(
                sendEhrExtractCoreTaskDefinition.getConversationId(), List.of(
                    EhrExtractStatus.GpcDocument.builder()
                        .documentId("")
                        .objectName(".gzip")
                        .fileName(".gzip")
                        .contentType("text/xml")
                        .isSkeleton(true)
                        .build()));
            outboundMessage.setPayload(structuredRecordMappingService
                .buildSkeletonEhrExtractXml(outboundMessage.getPayload(), "4"));
            outboundEhrExtract = new ObjectMapper().writeValueAsString(outboundMessage);
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

    private String replacePlaceholders(Map<String, String> replacements, String data) {
        StringSubstitutor sub = new StringSubstitutor(replacements);

        return sub.replace(data);
    }
}
