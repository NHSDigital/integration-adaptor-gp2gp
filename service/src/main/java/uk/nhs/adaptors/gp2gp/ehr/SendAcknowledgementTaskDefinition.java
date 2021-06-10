package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_ACKNOWLEDGEMENT;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

@Jacksonized
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
public class SendAcknowledgementTaskDefinition extends TaskDefinition {
    private final String nhsNumber;
    private final String typeCode;
    private final String ehrRequestMessageId;
    @Override
    public TaskType getTaskType() {
        return SEND_ACKNOWLEDGEMENT;
    }
}
