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
public class SendDocumentTaskExecutor implements TaskExecutor<SendDocumentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final MhsClient mhsClient;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final ObjectMapper objectMapper;
    private final DetectDocumentsSentService detectDocumentsSentService;

    @Override
    public Class<SendDocumentTaskDefinition> getTaskType() {
        return SendDocumentTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(SendDocumentTaskDefinition sendDocumentTaskDefinition) {
        LOGGER.info("SendDocument task was created, Sending EHR Document to GP");

        var storageDataWrapper = storageConnectorService.downloadFile(
            sendDocumentTaskDefinition.getDocumentName()
        );

        var outboundMessage = OutboundMessage.builder()
            .payload(storageDataWrapper.getData())
            .build();

        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);

        var messageId = randomIdGeneratorService.createNewId();

        var requestData =
            mhsRequestBuilder.buildSendEhrExtractCommonRequest(
                stringRequestBody,
                sendDocumentTaskDefinition.getConversationId(),
                sendDocumentTaskDefinition.getFromOdsCode(),
                messageId
            );

        mhsClient.sendMessageToMHS(requestData);

        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusCommon(sendDocumentTaskDefinition, messageId);

        detectDocumentsSentService.beginSendingPositiveAcknowledgement(ehrExtractStatus);
    }
}
