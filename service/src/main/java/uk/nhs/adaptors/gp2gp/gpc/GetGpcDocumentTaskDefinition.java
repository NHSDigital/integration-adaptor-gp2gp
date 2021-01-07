package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;

public class GetGpcDocumentTaskDefinition extends TaskDefinition {
    @Getter
    private final String documentId;

    @Autowired
    public GetGpcDocumentTaskDefinition(String taskId, String requestId, String conversationId, String documentId) {
        super(taskId, requestId, conversationId);
        this.documentId = documentId;
    }
}
