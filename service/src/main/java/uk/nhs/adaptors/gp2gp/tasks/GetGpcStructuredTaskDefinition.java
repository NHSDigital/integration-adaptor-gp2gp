package uk.nhs.adaptors.gp2gp.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

public class GetGpcStructuredTaskDefinition extends TaskDefinition{
    @Getter
    private String nhsNumber;

    @Autowired
    public GetGpcStructuredTaskDefinition(String requestId, String conversationId, String nhsNumber) {
        super(requestId, conversationId);
        this.nhsNumber = nhsNumber;
    }
}
