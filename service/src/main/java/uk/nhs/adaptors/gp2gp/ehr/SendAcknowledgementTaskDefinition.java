package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_ACKNOWLEDGEMENT;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
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
    @NonNull
    private final Optional<String> reasonCode;
    @NonNull
    private final Optional<String> detail;
    @Override
    public TaskType getTaskType() {
        return SEND_ACKNOWLEDGEMENT;
    }
}
