package uk.nhs.adaptors.gp2gp.tasks;

import static java.util.UUID.randomUUID;

import lombok.Getter;

@Getter
public abstract class TaskDefinition {
    private String taskId;
    private String requestId;
    private String conversationId;

    public TaskDefinition(String requestId, String conversationId) {
        this.taskId = randomUUID().toString();
        this.requestId = requestId;
        this.conversationId = conversationId;
    }
}
