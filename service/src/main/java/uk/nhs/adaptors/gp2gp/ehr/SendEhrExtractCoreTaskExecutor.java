package uk.nhs.adaptors.gp2gp.ehr;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;

import java.time.Instant;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFilenameConstants.GPC_STRUCTURED_FILE_EXTENSION;
import static uk.nhs.adaptors.gp2gp.gpc.GpcFilenameConstants.PATH_SEPARATOR;

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
        LOGGER.info("SendEhrExtractCore task was created, Sending EHR extract to Spine");

        String structuredRecordJsonFilename = sendEhrExtractCoreTaskDefinition.getConversationId()
            .concat(PATH_SEPARATOR)
            .concat(sendEhrExtractCoreTaskDefinition.getConversationId())
            .concat(GPC_STRUCTURED_FILE_EXTENSION);
        var storageDataWrapper = storageConnectorService.downloadFile(structuredRecordJsonFilename);

        var requestData =
            mhsRequestBuilder.buildSendEhrExtractCoreRequest(
                storageDataWrapper.getData(),
                sendEhrExtractCoreTaskDefinition.getConversationId(),
                sendEhrExtractCoreTaskDefinition.getFromOdsCode()
            );

        mhsClient.sendMessageToMHS(requestData);

        Instant requestSentAt = Instant.now();
        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusCore(sendEhrExtractCoreTaskDefinition, requestSentAt);
        if (ehrExtractStatus.getGpcAccessDocument().getDocuments().isEmpty()) {
            sendAcknowledgementTaskDispatcher.sendPositiveAcknowledgement(ehrExtractStatus);
        }
    }
}
