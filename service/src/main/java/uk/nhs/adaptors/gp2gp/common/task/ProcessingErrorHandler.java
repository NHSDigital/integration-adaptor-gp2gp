package uk.nhs.adaptors.gp2gp.common.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;

@Component
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ProcessingErrorHandler {

    private final ProcessFailureHandlingService processFailureHandlingService;

    public boolean handleRequestError(TaskDefinition taskDefinition) {

        return handleFailingProcess(
            taskDefinition,
            "18",
            "Request message not well-formed or not able to be processed"
        );
    }

    public boolean handleTranslationError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "10",
            "Failed to successfully generate EHR Extract."
        );
    }

    public boolean handleGeneralProcessingError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "99",
            "An error occurred when executing a task"
        );
    }

    public boolean handleGpConnectError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
            taskDefinition,
            "20",
            "Spine system responded with an error"
        );
    }

    public boolean handleInvalidNotAuthError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
                taskDefinition,
                "19",
                "Sender check indicates that Requester is not the patient’s current healthcare provider"
        );
    }
    public boolean handleNotFoundError(TaskDefinition taskDefinition) {
        return handleFailingProcess(
                taskDefinition,
                "6",
                "Patient not at surgery."
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
