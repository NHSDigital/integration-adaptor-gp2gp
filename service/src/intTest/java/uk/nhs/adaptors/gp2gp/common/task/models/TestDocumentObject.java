package uk.nhs.adaptors.gp2gp.common.task.models;

import lombok.Data;

@Data
public class TestDocumentObject {
    private String requestId;
    private String conversationId;
    private String documentId;

    public TestDocumentObject(String requestId, String conversationId, String documentId) {
        this.documentId = documentId;
        this.conversationId = conversationId;
        this.requestId = requestId;
    }
}
