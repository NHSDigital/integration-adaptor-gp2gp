package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_ABSENT_ATTACHMENT;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SendAbsentAttachmentTaskDefinition extends DocumentTaskDefinition {

    public SendAbsentAttachmentTaskDefinition(DocumentTaskDefinitionBuilder<?, ?> b, String title) {
        super(b);
        this.title = title;
    }

    /**
     * content.attachment.title of DocumentReference qualified to be an AbsentAttachment
     */
    private final String title;

    @Override
    public TaskType getTaskType() {
        return SEND_ABSENT_ATTACHMENT;
    }

}
