package uk.nhs.adaptors.gp2gp.ehr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class DocumentTaskDefinition extends TaskDefinition {
    /**
     * ID part of the URl pointing to the FHIR Binary resource hosted by GPC
     * or random UUID v4 if the task has been generated for the purpose of large messaging index fragment document.
     */
    private final String documentId;
    /**
     * TODO same as Conversation Id ??? need to check
     */
    private final String messageId;
}
