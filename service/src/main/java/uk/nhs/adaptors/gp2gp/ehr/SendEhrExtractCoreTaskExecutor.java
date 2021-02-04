package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessageWithPayload;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrExtractCoreTaskExecutor implements TaskExecutor<SendEhrExtractCoreTaskDefinition> {
    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final StorageConnectorService storageConnectorService;
    private final ObjectMapper objectMapper;

    @Override
    public Class<SendEhrExtractCoreTaskDefinition> getTaskType() {
        return SendEhrExtractCoreTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition) {
        LOGGER.info("SendEhrExtractCore task was created, Sending EHR extract to Spine");

        var storageDataWrapper = storageConnectorService.downloadFile(
            sendEhrExtractCoreTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        var extractCoreMessage = storageDataWrapper.getHl7Response();

        var outboundMessage = OutboundMessageWithPayload.builder().payload(extractCoreMessage).build();
        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(stringRequestBody);
        Instant requestSentAt = Instant.now();
        mhsClient.sendEhrExtractCore(request, sendEhrExtractCoreTaskDefinition);

        ehrExtractStatusService.updateEhrExtractStatusCore(sendEhrExtractCoreTaskDefinition, requestSentAt);
    }
}