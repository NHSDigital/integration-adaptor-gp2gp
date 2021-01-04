package uk.nhs.adaptors.gp2gp.common.task;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    private static final String TASK_TYPE_HEADER_NAME = "TaskName";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;

    public void handle(Message message) throws JMSException, JsonProcessingException, TaskHandlerException {
        var taskType = message.getStringProperty(TASK_TYPE_HEADER_NAME);
        var body = JmsReader.readMessage(message);
        LOGGER.info("Current task handled from internal task queue {}", taskType);

        Optional<TaskDefinition> taskDefinition = taskDefinitionFactory.getTaskDefinition(taskType, body);
        if (taskDefinition.isPresent()) {
            Optional<TaskExecutor> taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.get().getClass());
            if (taskExecutor.isPresent()) {
                taskExecutor.get().execute(taskDefinition.get());
            } else {
                LOGGER.error("No executor for task definition '{}' in message id '{}'", taskDefinition.get(), message.getJMSMessageID());
                throw new TaskHandlerException("No executor for task definition '" + taskDefinition.get() + "' in message id '"
                    + message.getJMSMessageID() + "'");
            }
        } else {
            LOGGER.error("Unknown task type '{}' in message id '{}'", taskType, message.getJMSMessageID());
            throw new TaskHandlerException("Unknown task type '" + taskType + "' in message id '" + message.getJMSMessageID() + "'");
        }
    }
}
