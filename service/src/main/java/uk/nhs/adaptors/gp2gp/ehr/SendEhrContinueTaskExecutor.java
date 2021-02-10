package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrContinueTaskExecutor implements TaskExecutor<SendEhrContinueTaskDefinition> {
    @Override
    public Class<SendEhrContinueTaskDefinition> getTaskType() {
        return SendEhrContinueTaskDefinition.class;
    }

    @Override
    public void execute(SendEhrContinueTaskDefinition taskDefinition) {
        LOGGER.info("SendEhrContinue task was created, Sending EHR Continue to GP");
        LOGGER.info("Document: " + taskDefinition.getDocumentName());
    }
}
