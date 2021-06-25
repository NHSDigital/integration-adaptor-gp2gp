package uk.nhs.adaptors.gp2gp.ehr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class DocumentTaskDefinition extends TaskDefinition {
    private final String documentId;
    private final String messageId;
}
