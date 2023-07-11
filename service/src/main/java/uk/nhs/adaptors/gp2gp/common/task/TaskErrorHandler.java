package uk.nhs.adaptors.gp2gp.common.task;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.exception.MaximumExternalAttachmentsException;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.gpc.exception.EhrRequestException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;

@Component
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class TaskErrorHandler {

    private final Map<Class<? extends Exception>, Function<TaskDefinition, Boolean>> errorHandlers = Map.of(
        EhrRequestException.class, this::handleRequestError,
        EhrExtractException.class, this::handleTranslationError,
        EhrMapperException.class, this::handleTranslationError,
        FhirValidationException.class, this::handleTranslationError,
        GpConnectException.class, this::handleGpConnectError,
        GpConnectInvalidException.class, this::handleInvalidNotAuthError,
        GpConnectNotFoundException.class, this::handleNotFoundError,
        MaximumExternalAttachmentsException.class, this::handleMaximumExternalAttachmentsError
    );

    private final ProcessFailureHandlingService processFailureHandlingService;

    public boolean handleProcessingError(Exception exception, TaskDefinition taskDefinition) {
        return errorHandlers.getOrDefault(exception.getClass(), this::handleGeneralProcessingError).apply(taskDefinition);
    }

    private boolean handleRequestError(TaskDefinition taskDefinition) {

        return handleFailingProcess(
            taskDefinition,
            "18",
            "Request message not well-formed or not able to be processed"
        );
    }

    private boolean handleTranslationError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "10",
            "Failed to successfully generate EHR Extract."
        );
    }

    private boolean handleGeneralProcessingError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "99",
            "An error occurred when executing a task"
        );
    }

    private boolean handleGpConnectError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "20",
            "Spine system responded with an error"
        );
    }

    private boolean handleInvalidNotAuthError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
                taskDefinition,
                "19",
                "Sender check indicates that Requester is not the patient’s current healthcare provider"
        );
    }
    private boolean handleNotFoundError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
                taskDefinition,
                "06",
                "Patient not at surgery."
        );
    }

    private boolean handleMaximumExternalAttachmentsError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "30",
            "Large Message general failure"
        );
    }

    private boolean isNotSendNackTask(TaskDefinition taskDefinition) {
        return !(
            TaskType.SEND_ACKNOWLEDGEMENT.equals(taskDefinition.getTaskType())
                && ((SendAcknowledgementTaskDefinition) taskDefinition).isNack());
    }

    private boolean handleFailingProcess(TaskDefinition taskDefinition, String errorCode, String errorMessage) {
        if (taskDefinition != null && isNotSendNackTask(taskDefinition)) {
            return processFailureHandlingService.failProcess(
                taskDefinition.getConversationId(),
                errorCode,
                errorMessage,
                taskDefinition.getTaskType().name()
            );
        }

        return false;
    }
}
