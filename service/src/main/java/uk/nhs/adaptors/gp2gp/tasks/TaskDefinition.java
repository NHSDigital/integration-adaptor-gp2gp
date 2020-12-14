package uk.nhs.adaptors.gp2gp.tasks;

import static java.util.UUID.randomUUID;

import lombok.Getter;

@Getter
public abstract class TaskDefinition {
    private final String taskId;
    private final String requestId;
    private final String conversationId;

    public TaskDefinition(String requestId, String conversationId) {
        this.taskId = randomUUID().toString();
        this.requestId = requestId;
        this.conversationId = conversationId;
    }
}
