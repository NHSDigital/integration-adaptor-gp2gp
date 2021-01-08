package uk.nhs.adaptors.gp2gp.common.task;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    public static final String TASK_TYPE_HEADER_NAME = "TaskType";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handle(Message message) {
        String taskType = null;
        String body = null;
        try {
            taskType = message.getStringProperty(TASK_TYPE_HEADER_NAME);
            body = JmsReader.readMessage(message);
        } catch (JMSException e) {
            throw new TaskHandlerException("Unable to read task definition from JSM message", e);
        }
        LOGGER.info("Received a task of type {} on internal task queue", taskType);

        TaskDefinition taskDefinition = taskDefinitionFactory.getTaskDefinition(taskType, body);

        TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.getClass());

        taskExecutor.execute(taskDefinition);
    }
}
