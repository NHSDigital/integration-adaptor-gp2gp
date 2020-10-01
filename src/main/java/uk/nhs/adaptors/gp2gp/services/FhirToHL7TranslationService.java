package uk.nhs.adaptors.gp2gp.services;

import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.stereotype.Component;

@Component
public class FhirToHL7TranslationService {
    public String translate(Bundle gpConnectResponse) {
        return "<root/>";
    }
}
