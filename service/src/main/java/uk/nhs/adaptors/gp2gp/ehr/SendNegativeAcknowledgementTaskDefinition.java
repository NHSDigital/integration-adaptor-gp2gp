package uk.nhs.adaptors.gp2gp.ehr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_NEGATIVE_ACKNOWLEDGEMENT;

@Jacksonized
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
public class SendNegativeAcknowledgementTaskDefinition extends TaskDefinition {

    private final String reasonCode;
    private final String reasonMessage;
    private final String ehrRequestMessageId;

    @Override
    public TaskType getTaskType() {
        return SEND_NEGATIVE_ACKNOWLEDGEMENT;
    }
}
