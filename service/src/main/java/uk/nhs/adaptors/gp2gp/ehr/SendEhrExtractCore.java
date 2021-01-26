package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskIdService;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SendEhrExtractCore {

    private final TaskDispatcher taskDispatcher;
    private final TaskIdService taskIdService;

    public void send(EhrExtractStatus ehrExtractStatus) {
        var sendEhrExtractCoreTaskDefinition = SendEhrExtractCoreTaskDefinition.builder()
            .taskId(taskIdService.createNewTaskId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .build();

        taskDispatcher.createTask(sendEhrExtractCoreTaskDefinition);
    }
}
