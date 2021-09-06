package uk.nhs.adaptors.gp2gp.ehr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_EHR_CONTINUE;

@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SendDocumentTaskDefinition extends DocumentTaskDefinition {
    private final String documentName;
    private final int documentPosition;
    @Builder.Default
    private final boolean externalEhrExtract = false;
    @Override
    public TaskType getTaskType() {
        return SEND_EHR_CONTINUE;
    }
}
