package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;

public class GetGpcStructuredTaskDefinition extends TaskDefinition {
    @Getter
    private final String nhsNumber;

    @Autowired
    public GetGpcStructuredTaskDefinition(String taskId, String requestId, String conversationId, String nhsNumber) {
        super(taskId, requestId, conversationId);
        this.nhsNumber = nhsNumber;
    }
}
