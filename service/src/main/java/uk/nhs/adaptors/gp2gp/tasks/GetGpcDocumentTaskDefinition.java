package uk.nhs.adaptors.gp2gp.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

public class GetGpcDocumentTaskDefinition extends TaskDefinition{
    @Getter
    private String documentId;

    @Autowired
    public GetGpcDocumentTaskDefinition(String requestId, String conversationId, String documentId) {
        super(requestId, conversationId);
        this.documentId = documentId;
    }
}
