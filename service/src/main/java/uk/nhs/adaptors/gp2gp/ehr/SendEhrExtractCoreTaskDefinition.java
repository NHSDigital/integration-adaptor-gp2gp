package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_EHR_EXTRACT_CORE;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

/**
 * Task definition for sending EHR extract to winning practice via MHS
 */
@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SendEhrExtractCoreTaskDefinition extends TaskDefinition {

    private String ehrExtractMessageId;

    @Override
    public TaskType getTaskType() {
        return SEND_EHR_EXTRACT_CORE;
    }
}
