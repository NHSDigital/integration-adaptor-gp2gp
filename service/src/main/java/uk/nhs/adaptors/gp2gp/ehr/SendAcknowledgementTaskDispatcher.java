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
public class SendAcknowledgementTaskDispatcher {
    private final TaskDispatcher taskDispatcher;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public void send(EhrExtractStatus ehrExtractStatus, String typeCode) {
        var sendAcknowledgementTaskDefinition = SendAcknowledgementTaskDefinition.builder()
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
            .typeCode(typeCode)
            .build();

        taskDispatcher.createTask(sendAcknowledgementTaskDefinition);
        LOGGER.info(String.format("SendAcknowledgementTaskDefiniiton added to task queue, conversationId: %s",
            ehrExtractStatus.getConversationId()));
    }
}
