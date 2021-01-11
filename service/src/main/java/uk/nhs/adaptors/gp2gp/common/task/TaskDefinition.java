package uk.nhs.adaptors.gp2gp.common.task;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class TaskDefinition {
    private final String taskId;
    private final String requestId;
    private final String conversationId;

    public abstract TaskType getTaskType();
}
