package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.PractitionerAgentPersonMapperParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AgentPersonMapper {

    private static final Mustache AGENT_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_agent_person_template.mustache");
    private static final String UNKNOWN = "Unknown";
    private final MessageContext messageContext;

    public String mapAgentPerson(AgentDirectory.AgentKey agentKey, String agentDirectoryId) {
        var builder = PractitionerAgentPersonMapperParameters.builder()
            .agentId(agentDirectoryId);

        Optional<Practitioner> practitioner = messageContext.getInputBundleHolder()
            .getResource(new IdType(agentKey.getPractitionerReference()))
            .map(resource -> (Practitioner) resource);

        if (practitioner.isPresent()) {
            builder.practitioner(true);
            buildPractitionerPrefix(practitioner.get()).ifPresent(builder::practitionerPrefix);

            var practitionerGiven = buildPractitionerGivenName(practitioner.get());
            var practitionerFamily = buildPractitionerFamilyName(practitioner.get());

            practitionerGiven.ifPresent(builder::practitionerGivenName);
            practitionerFamily.ifPresent(builder::practitionerFamilyName);

            if (practitionerGiven.isEmpty() && practitionerFamily.isEmpty()) {
                builder.practitionerFamilyName(UNKNOWN);
            }
        }

        buildPractitionerRole(agentKey).ifPresent(builder::practitionerRole);

        messageContext.getInputBundleHolder()
            .getResource(new IdType(agentKey.getOrganizationReference()))
            .map(resource -> (Organization) resource)
            .map(OrganizationToAgentMapper::mapOrganizationToAgentInner)
            .ifPresent(builder::organization);

        return TemplateUtils.fillTemplate(AGENT_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildPractitionerRole(AgentDirectory.AgentKey agentKey) {
        Optional<PractitionerRole> practitionerRole = messageContext.getInputBundleHolder()
            .getPractitionerRoleFor(
                agentKey.getPractitionerReference(),
                agentKey.getOrganizationReference()
            );

        if (practitionerRole.isPresent() && hasPractitionerRoleDisplay(practitionerRole.get())) {
            return Optional.of(practitionerRole.get().getCodeFirstRep().getCodingFirstRep().getDisplay());
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
