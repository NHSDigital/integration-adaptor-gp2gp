package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrCommonTaskExecutor implements TaskExecutor<SendEhrCommonTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final MhsClient mhsClient;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final ObjectMapper objectMapper;

    @Override
    public Class<SendEhrCommonTaskDefinition> getTaskType() {
        return SendEhrCommonTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(SendEhrCommonTaskDefinition taskDefinition) {
        LOGGER.info("SendEhrCommon task was created, Sending EHR Common to GP");

        var storageDataWrapper = storageConnectorService.downloadFile(taskDefinition.getDocumentName());
        var messageId = randomIdGeneratorService.createNewId();

        var outboundMessage = OutboundMessage.builder()
            .payload(storageDataWrapper.getData())
            .build();

        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);

        var requestData = mhsRequestBuilder
            .buildSendEhrExtractCommonRequest(
                stringRequestBody,
                taskDefinition.getConversationId(),
                taskDefinition.getFromOdsCode(),
                messageId);

        mhsClient.sendMessageToMHS(requestData);

        ehrExtractStatusService.updateEhrExtractStatusCommon(taskDefinition, messageId);
    }
}
