package uk.nhs.adaptors.gp2gp.ehr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_EHR_CONTINUE;

/**
 * Task definition for sending documents to winning practice via MHS
 */
@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SendDocumentTaskDefinition extends DocumentTaskDefinition {
    /**
     * File name of the document
     */
    private final String documentName;
    /**
     * Position of the document on EhrStatus document list
     */
    private final int documentPosition;
    @Override
    public TaskType getTaskType() {
        return SEND_EHR_CONTINUE;
    }
}
