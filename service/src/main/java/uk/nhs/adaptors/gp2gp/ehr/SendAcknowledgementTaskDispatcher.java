package uk.nhs.adaptors.gp2gp.ehr;

import java.util.Optional;

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
public class SendAcknowledgementTaskDispatcher {
    private static final String POSITIVE_ACKNOWLEDGEMENT_TYPE_CODE = "AA";
    private final TaskDispatcher taskDispatcher;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public void sendPositiveAcknowledgement(EhrExtractStatus ehrExtractStatus) {
        var ehrRequest = ehrExtractStatus.getEhrRequest();
        var sendAcknowledgementTaskDefinition = SendAcknowledgementTaskDefinition.builder()
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrRequest.getRequestId())
            .toAsid(ehrRequest.getToAsid())
            .fromAsid(ehrRequest.getFromAsid())
            .fromOdsCode(ehrRequest.getFromOdsCode())
            .toOdsCode(ehrRequest.getToOdsCode())
            .nhsNumber(ehrRequest.getNhsNumber())
            .typeCode(POSITIVE_ACKNOWLEDGEMENT_TYPE_CODE)
            .reasonCode(Optional.empty())
            .detail(Optional.empty())
            .messageId(ehrRequest.getMessageId())
            .build();

        taskDispatcher.createTask(sendAcknowledgementTaskDefinition);
        LOGGER.info("SendAcknowledgementTaskDefinition added to task queue");
    }
}
