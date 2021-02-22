package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.PractitionerAgentPersonMapperParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class PractitionerAgentPersonMapper {

    private static final Mustache AGENT_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_agent_person_template.mustache");
    private final MessageContext messageContext;

    public String mapPractitionerToAgentPerson(PractitionerRole practitionerRole, Practitioner practitioner, Organization organization) {
        var builder = PractitionerAgentPersonMapperParameters.builder()
            .practitionerId(messageContext.getIdMapper().getOrNew(ResourceType.Practitioner, practitioner.getIdElement().getIdPart()));

        buildPractitionerRole(practitionerRole).ifPresent(builder::practitionerRole);

        return TemplateUtils.fillTemplate(AGENT_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildPractitionerRole(PractitionerRole practitionerRole) {
        return Optional.empty();
    }

    private Optional<String> buildPractitionerPrefix(PractitionerRole practitionerRole) {
        return Optional.empty();
    }

    private Optional<String> buildPractitionerGivenName(PractitionerRole practitionerRole) {
        return Optional.empty();
    }

    private Optional<String> buildPractitionerFamilyName(PractitionerRole practitionerRole) {
        return Optional.empty();
    }

}
