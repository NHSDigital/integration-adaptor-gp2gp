package uk.nhs.adaptors.gp2gp.services;

import com.github.mustachejava.Mustache;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.utils.TemplateUtils;

@Component
public class FhirToHL7TranslationService {

    private static final Mustache RCMR_IN030000UK06_TEMPLATE = TemplateUtils.loadTemplate("RCMR_IN030000UK06.mustache");

    public String translate(Bundle gpConnectResponse) {
        return TemplateUtils.fillTemplate(RCMR_IN030000UK06_TEMPLATE, new Object());
    }
}
