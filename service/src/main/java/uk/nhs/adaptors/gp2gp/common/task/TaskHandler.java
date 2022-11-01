package uk.nhs.adaptors.gp2gp.common.task;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.gpc.exception.EhrRequestException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    public static final String TASK_TYPE_HEADER_NAME = "TaskType";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;
    private final MDCService mdcService;
    private final ProcessFailureHandlingService processFailureHandlingService;
    private final ProcessingErrorHandler processingErrorHandler;

    /**
     * @return True if the message has been processed. Otherwise, false.
     */
    @SneakyThrows
    public boolean handle(Message message) throws DataAccessResourceFailureException {
        TaskDefinition taskDefinition = null;

        try {
            taskDefinition = readTaskDefinition(message);

            if (!processFailureHandlingService.hasProcessFailed(taskDefinition.getConversationId())
                || isSendNackTask(taskDefinition)) {
                executeTask(taskDefinition);
            } else {
                LOGGER.warn(
                    "Aborting the execution of task {} for conversation_id: {}. The process has already failed",
                    taskDefinition.getTaskType().name(),
                    taskDefinition.getConversationId()
                );
            }

            return true;

        } catch (DataAccessResourceFailureException e) {
            logError(e, message);
            throw e;
        } catch (TaskHandlerException e) {
            logError(e, message);
            return false;
        } catch (EhrRequestException e) {
            logError(e, message);
            return processingErrorHandler.handleRequestError(taskDefinition);
        } catch (EhrExtractException | EhrMapperException | FhirValidationException e) {
            logError(e, message);
            return processingErrorHandler.handleTranslationError(taskDefinition);
        } catch (GpConnectException e) {
            logError(e, message);
            return processingErrorHandler.handleGpConnectError(taskDefinition);
        } catch (GpConnectInvalidException e) {
            logError(e, message);
            return processingErrorHandler.handleInvalidNotAuthError(taskDefinition);
        } catch (GpConnectNotFoundException e) {
            logError(e, message);
            return processingErrorHandler.handleNotFoundError(taskDefinition);
        } catch (Exception e) {
            logError(e, message);
            return processingErrorHandler.handleGeneralProcessingError(taskDefinition);
        }
    }
    @SneakyThrows
    private void logError(Exception e, Message message) {
        LOGGER.error("An error occurred while handing a task message_id: {}", message.getJMSMessageID(), e);
    }

    private TaskDefinition readTaskDefinition(Message message) {
        try {
            String taskType = message.getStringProperty(TASK_TYPE_HEADER_NAME);
            String body = JmsReader.readMessage(message);

            LOGGER.info("Message taskType: {}", taskType);
            return taskDefinitionFactory.getTaskDefinition(taskType, body);
        } catch (JMSException e) {
            throw new TaskHandlerException("Unable to read task definition from JMS message", e);
        }
    }
    @SuppressWarnings({"unchecked"})
    private void executeTask(TaskDefinition taskDefinition) {
        mdcService.applyConversationId(taskDefinition.getConversationId());
        mdcService.applyTaskId(taskDefinition.getTaskId());

        var taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.getClass());

        LOGGER.info("Executing {}", taskExecutor.getClass().getName());

        taskExecutor.execute(taskDefinition);
    }

    private boolean isSendNackTask(TaskDefinition taskDefinition) {
        return TaskType.SEND_ACKNOWLEDGEMENT.equals(taskDefinition.getTaskType())
            && ((SendAcknowledgementTaskDefinition) taskDefinition).isNack();
    }
}
