package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_STRUCTURED;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

/**
 * Task definition for downloading Structured Record from GCP
 */
@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetGpcStructuredTaskDefinition extends TaskDefinition {
    /**
     * Value from {@code /RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/recordTarget/patient/id/@extension} of SOAP message payload
     */
    private final String nhsNumber;

    @Override
    public TaskType getTaskType() {
        return GET_GPC_STRUCTURED;
    }
}
