package uk.nhs.adaptors.gp2gp.ehr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendNegativeAcknowledgementExecutor implements TaskExecutor<SendNegativeAcknowledgementTaskDefinition> {
    @Override
    public Class<SendNegativeAcknowledgementTaskDefinition> getTaskType() {
        return SendNegativeAcknowledgementTaskDefinition.class;
    }

    @Override
    public void execute(SendNegativeAcknowledgementTaskDefinition taskDefinition) {
        // TODO: implement as part of NIAD-846
        LOGGER.info("Sending NACK message. Conversation ID: {}", taskDefinition.getConversationId());
    }
}
