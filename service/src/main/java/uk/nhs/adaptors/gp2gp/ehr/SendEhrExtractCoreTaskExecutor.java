package uk.nhs.adaptors.gp2gp.ehr;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.gpc.GpcFilenameUtils;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrExtractCoreTaskExecutor implements TaskExecutor<SendEhrExtractCoreTaskDefinition> {
    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final StorageConnectorService storageConnectorService;
    private final SendAcknowledgementTaskDispatcher sendAcknowledgementTaskDispatcher;

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

        var requestData = mhsRequestBuilder
            .buildSendEhrExtractCoreRequest(
                replacePlaceholders(documentObjectNameAndSize, storageDataWrapper.getData()),
                sendEhrExtractCoreTaskDefinition.getConversationId(),
                sendEhrExtractCoreTaskDefinition.getFromOdsCode(),
                sendEhrExtractCoreTaskDefinition.getEhrExtractMessageId()
            );

        Instant requestSentAtPending = Instant.now();
        ehrExtractStatusService.updateEhrExtractStatusCorePending(sendEhrExtractCoreTaskDefinition, requestSentAtPending);

        mhsClient.sendMessageToMHS(requestData);

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
