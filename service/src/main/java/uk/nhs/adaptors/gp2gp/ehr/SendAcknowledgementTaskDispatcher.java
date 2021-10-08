package uk.nhs.adaptors.gp2gp.ehr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import static uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition.ACK_TYPE_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition.NACK_TYPE_CODE;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SendAcknowledgementTaskDispatcher {
    private final TaskDispatcher taskDispatcher;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public void sendPositiveAcknowledgement(EhrExtractStatus ehrExtractStatus) {
        SendAcknowledgementTaskDefinition.SendAcknowledgementTaskDefinitionBuilder<?, ?> taskDefinitionBuilder =
            getAcknowledgementTaskDefinitionBuilder(ehrExtractStatus, ACK_TYPE_CODE);

        taskDispatcher.createTask(taskDefinitionBuilder.build());
        LOGGER.info("Sent {} task with ACK: {}", SendAcknowledgementTaskDefinition.class.getSimpleName(), ACK_TYPE_CODE);
    }

    public void sendNegativeAcknowledgement(EhrExtractStatus ehrExtractStatus, String reasonCode, String reasonMessage) {
        SendAcknowledgementTaskDefinition.SendAcknowledgementTaskDefinitionBuilder<?, ?> taskDefinitionBuilder =
            getAcknowledgementTaskDefinitionBuilder(ehrExtractStatus, NACK_TYPE_CODE);
        taskDefinitionBuilder.reasonCode(reasonCode);
        taskDefinitionBuilder.detail(reasonMessage);

        taskDispatcher.createTask(taskDefinitionBuilder.build());
        LOGGER.info("Sent {} task with NACK: {}", SendAcknowledgementTaskDefinition.class.getSimpleName(), NACK_TYPE_CODE);
    }

    private SendAcknowledgementTaskDefinition.SendAcknowledgementTaskDefinitionBuilder<?, ?> getAcknowledgementTaskDefinitionBuilder(
        EhrExtractStatus ehrExtractStatus, String typeCode) {

        var ehrRequest = ehrExtractStatus.getEhrRequest();
        return SendAcknowledgementTaskDefinition.builder()
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrRequest.getRequestId())
            .toAsid(ehrRequest.getToAsid())
            .fromAsid(ehrRequest.getFromAsid())
            .fromOdsCode(ehrRequest.getFromOdsCode())
            .toOdsCode(ehrRequest.getToOdsCode())
            .nhsNumber(ehrRequest.getNhsNumber())
            .typeCode(typeCode)
            .ehrRequestMessageId(ehrRequest.getMessageId());
    }
}
