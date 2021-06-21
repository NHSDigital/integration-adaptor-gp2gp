package uk.nhs.adaptors.gp2gp.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDispatcher;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProcessFailureHandlingService {

    private final SendAcknowledgementTaskDispatcher sendAcknowledgementTaskDispatcher;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final EhrExtractStatusRepository ehrExtractStatusRepository;

    /**
     * Closes the process for the given conversation ID as failed
     *
     * @param errorCode Code representing the error category
     * @param errorMessage High level error message
     * @param taskType Type of the task in which the error occurred
     * @return True when the process was successfully closed as failed. Otherwise, false.
     */
    public boolean failProcess(String conversationId, String errorCode, String errorMessage, String taskType) {
        try {
            return this.ehrExtractStatusRepository.findByConversationId(conversationId)
                .map(ehrExtractStatus -> {
                    ehrExtractStatusService.updateEhrExtractStatusError(conversationId, errorCode, errorMessage, taskType);
                    sendAcknowledgementTaskDispatcher.sendNegativeAcknowledgement(ehrExtractStatus, errorCode, errorMessage);
                    return true;
                })
                .orElseGet(() -> {
                    LOGGER.warn("EHR extract status not found for Conversation-Id {}. Can't update it with error information.");
                    return false;
                });
        } catch (Exception e) {
            LOGGER.error("An error occurred when closing a failed process for Conversation-Id: {}", conversationId, e);
            return false;
        }
    }

    public boolean hasProcessFailed(String conversationId) {
        return this.ehrExtractStatusRepository.findByConversationId(conversationId)
            .map(ehrExtractStatus -> ehrExtractStatus.getError() != null)
            .orElse(false);
    }
}
