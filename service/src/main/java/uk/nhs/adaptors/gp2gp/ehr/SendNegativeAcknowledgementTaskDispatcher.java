package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SendNegativeAcknowledgementTaskDispatcher {
    private static final String NACK_TYPE_CODE = "AE";

    private final TaskDispatcher taskDispatcher;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public void sendNegativeAcknowledgement(EhrExtractStatus ehrExtractStatus, String reasonCode, String reasonMessage) {
        var ehrRequest = ehrExtractStatus.getEhrRequest();
        var sendAcknowledgementTaskDefinition = SendNegativeAcknowledgementTaskDefinition.builder()
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrRequest.getRequestId())
            .toAsid(ehrRequest.getToAsid())
            .fromAsid(ehrRequest.getFromAsid())
            .fromOdsCode(ehrRequest.getFromOdsCode())
            .toOdsCode(ehrRequest.getToOdsCode())
            .reasonCode(reasonCode)
            .detail(reasonMessage)
            .typeCode(NACK_TYPE_CODE)
            .ehrRequestMessageId(ehrRequest.getMessageId())
            .build();

        taskDispatcher.createTask(sendAcknowledgementTaskDefinition);

        LOGGER.info(
            "SendNegativeAcknowledgementTask added to task queue for Conversation-Id {}",
            ehrExtractStatus.getConversationId()
        );
    }
}
