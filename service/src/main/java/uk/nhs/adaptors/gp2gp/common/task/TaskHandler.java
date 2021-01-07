package uk.nhs.adaptors.gp2gp.common.task;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.utils.ConversationIdService;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    private static final String TASK_TYPE_HEADER_NAME = "TaskName";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;
    private final ConversationIdService conversationIdService;
    private final TaskIdService taskIdService;

    public void handle(Message message) throws JMSException, JsonProcessingException, TaskHandlerException {
        var taskType = message.getStringProperty(TASK_TYPE_HEADER_NAME);
        var body = JmsReader.readMessage(message);
        LOGGER.info("Current task handled from internal task queue {}", taskType);

        TaskDefinition taskDefinition = taskDefinitionFactory.getTaskDefinition(taskType, body);

        conversationIdService.applyConversationId(taskDefinition.getConversationId());
        taskIdService.applyTaskId(taskDefinition.getTaskId());

        LOGGER.info("Current task defined from internal task queue {}", taskType);

        TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.getClass());

        taskExecutor.execute(taskDefinition);
    }
}
