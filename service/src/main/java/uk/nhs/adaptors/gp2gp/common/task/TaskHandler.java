package uk.nhs.adaptors.gp2gp.common.task;

import javax.jms.JMSException;
import javax.jms.Message;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    public static final String TASK_TYPE_HEADER_NAME = "TaskType";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;
    private final MDCService mdcService;
    private final ProcessFailureHandlingService processFailureHandlingService;

    /**
     * @return True if the message has been processed. Otherwise, false.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @SneakyThrows
    public boolean handle(Message message) {
        TaskDefinition taskDefinition = null;

        try {
            taskDefinition = readTaskDefinition(message);

            if (!processFailureHandlingService.hasProcessFailed(taskDefinition.getConversationId())) {
                executeTask(taskDefinition);
            } else {
                LOGGER.warn(
                    "Aborting the execution of task {} for Conversation-Id: {}. The process has already failed",
                    taskDefinition.getTaskType().name(),
                    taskDefinition.getConversationId()
                );
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("An error occurred while handing a task message {}", message.getJMSMessageID(), e);
            return handleProcessingError(taskDefinition);
        }
    }

    private TaskDefinition readTaskDefinition(Message message) {
        try {
            String taskType = message.getStringProperty(TASK_TYPE_HEADER_NAME);
            String body = JmsReader.readMessage(message);

            LOGGER.info("Received a message on the task queue with {} header {}", TASK_TYPE_HEADER_NAME, taskType);
            return taskDefinitionFactory.getTaskDefinition(taskType, body);
        } catch (JMSException e) {
            throw new TaskHandlerException("Unable to read task definition from JMS message", e);
        }
    }

    private void executeTask(TaskDefinition taskDefinition) {
        mdcService.applyConversationId(taskDefinition.getConversationId());
        mdcService.applyTaskId(taskDefinition.getTaskId());

        TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.getClass());

        LOGGER.info("Executing a {} with parameters from a {}", taskExecutor.getClass(), taskDefinition.getClass());

        taskExecutor.execute(taskDefinition);
    }

    private boolean handleProcessingError(TaskDefinition taskDefinition) {
        if (taskDefinition != null && !TaskType.SEND_NEGATIVE_ACKNOWLEDGEMENT.equals(taskDefinition.getTaskType())) {
            return processFailureHandlingService.failProcess(
                taskDefinition.getConversationId(),
                // TODO: error code and message to be prepared as part of NIAD-1524
                "18",
                "An error occurred when executing a task",
                taskDefinition.getTaskType().name()
            );
        } else {
            return false;
        }
    }
}
