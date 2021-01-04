package uk.nhs.adaptors.gp2gp.common.task;

import lombok.Getter;

@Getter
public abstract class TaskDefinition {
    private final String taskId;
    private final String requestId;
    private final String conversationId;

    public TaskDefinition(String taskId, String requestId, String conversationId) {
        this.taskId = taskId;
        this.requestId = requestId;
        this.conversationId = conversationId;
    }
}
