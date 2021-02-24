package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

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
    private static final String UNKNOWN = "Unknown";
    private final MessageContext messageContext;
    private final OrganizationToAgentMapper organizationToAgentMapper;

    public String mapPractitionerToAgentPerson(Practitioner practitioner, Optional<PractitionerRole> practitionerRole,
        Optional<Organization> organization) {
        var builder = PractitionerAgentPersonMapperParameters.builder()
            .practitionerId(messageContext.getIdMapper().getOrNew(ResourceType.Practitioner, practitioner.getIdElement().getIdPart()));

        buildPractitionerPrefix(practitioner).ifPresent(builder::practitionerPrefix);
        var practitionerGiven = buildPractitionerGivenName(practitioner);
        var practitionerFamily = buildPractitionerFamilyName(practitioner);

        practitionerGiven.ifPresent(builder::practitionerGivenName);
        practitionerFamily.ifPresent(builder::practitionerFamilyName);

        if (practitionerGiven.isEmpty() && practitionerFamily.isEmpty()) {
            builder.practitionerFamilyName(UNKNOWN);
        }

        practitionerRole.flatMap(this::buildPractitionerRole).ifPresent(builder::practitionerRole);
        organization.map(organizationToAgentMapper::mapOrganizationToAgentInner).ifPresent(builder::organization);

        return TemplateUtils.fillTemplate(AGENT_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildPractitionerRole(PractitionerRole practitionerRole) {
        if (hasPractitionerRoleDisplay(practitionerRole)) {
            return Optional.of(practitionerRole.getCodeFirstRep().getCodingFirstRep().getDisplay());
        }
        return Optional.empty();
    }

    private Optional<String> buildPractitionerPrefix(Practitioner practitioner) {
        if (practitioner.hasName() && practitioner.getNameFirstRep().hasPrefix()) {
            return Optional.of(practitioner.getNameFirstRep().getPrefixAsSingleString());
        }
        return Optional.empty();
    }

    private Optional<String> buildPractitionerGivenName(Practitioner practitioner) {
        if (practitioner.hasName() && practitioner.getNameFirstRep().hasGiven()) {
            return Optional.of(practitioner.getNameFirstRep().getGivenAsSingleString());
        }
        return Optional.empty();
    }

    private Optional<String> buildPractitionerFamilyName(Practitioner practitioner) {
        if (practitioner.hasName() && practitioner.getNameFirstRep().hasFamily()) {
            return Optional.of(practitioner.getNameFirstRep().getFamily());
        }
        return Optional.empty();
    }

    private static boolean hasPractitionerRoleDisplay(PractitionerRole practitionerRole) {
        return practitionerRole.hasCode()
            && practitionerRole.getCodeFirstRep().hasCoding()
            && practitionerRole.getCodeFirstRep().getCodingFirstRep().hasDisplay();
    }
}
