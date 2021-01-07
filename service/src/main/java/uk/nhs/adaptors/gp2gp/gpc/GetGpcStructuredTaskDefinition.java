package uk.nhs.adaptors.gp2gp.gpc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetGpcStructuredTaskDefinition extends TaskDefinition {
    private final String nhsNumber;

    @Override
    public TaskType getTaskType() {
        return TaskType.GET_GPC_STRUCTURED;
    }
}
