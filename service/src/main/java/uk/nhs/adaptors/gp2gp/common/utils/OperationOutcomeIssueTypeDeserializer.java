package uk.nhs.adaptors.gp2gp.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
import org.hl7.fhir.exceptions.FHIRException;

import java.io.IOException;

public class OperationOutcomeIssueTypeDeserializer extends JsonDeserializer<IssueType> {

    @Override
    public IssueType deserialize(JsonParser p, DeserializationContext context) throws IOException {
        try {
            return IssueType.fromCode(p.getText());
        } catch (FHIRException e) {
            throw new IOException("Failed to deserialize OperationOutcomeIssueType value: " + p.getText(), e);
        }
    }
}