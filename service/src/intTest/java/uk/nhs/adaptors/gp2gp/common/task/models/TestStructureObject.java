package uk.nhs.adaptors.gp2gp.common.task.models;

import lombok.Data;

@Data
public class TestStructureObject {
    private String requestId;
    private String conversationId;
    private String nhsNumber;

    public TestStructureObject(String requestId, String conversationId, String nhsNumber) {
        this.nhsNumber = nhsNumber;
        this.conversationId = conversationId;
        this.requestId = requestId;
    }
}
