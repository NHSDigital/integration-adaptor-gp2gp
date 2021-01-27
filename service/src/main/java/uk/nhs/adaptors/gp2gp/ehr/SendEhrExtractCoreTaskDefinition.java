package uk.nhs.adaptors.gp2gp.ehr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SendEhrExtractCoreTaskDefinition extends TaskDefinition {
    @Override
    public TaskType getTaskType() {
        return TaskType.SEND_EHR_EXTRACT_CORE;
    }
}
