package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
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
        var builder = PractitionerAgentPersonMapperParameters
                .builder()
                .agentId(agentDirectoryId);

        AtomicReference<String> tempNameFromText = new AtomicReference<>("");

        if (agentKey.getPractitionerReference() != null) {
            messageContext.getInputBundleHolder()
                .getResource(new IdType(agentKey.getPractitionerReference()))
                .map(Practitioner.class::cast)
                .ifPresent(
                    practitioner -> {
                        builder.practitioner(true);
                        buildPractitionerPrefix(practitioner).ifPresent(builder::practitionerPrefix);

                        var practitionerGiven = buildPractitionerGivenName(practitioner);
                        var practitionerFamily = buildPractitionerFamilyName(practitioner);
                        var practitionerUnstructuredName = buildPractitionerUnstructuredName(practitioner);

                        practitionerGiven.ifPresent(builder::practitionerGivenName);
                        practitionerFamily.ifPresent(builder::practitionerFamilyName);

                        if (practitionerGiven.isEmpty() && practitionerFamily.isEmpty()) {
                            practitionerUnstructuredName.ifPresentOrElse
                                (builder::practitionerUnstructuredName, () -> builder.practitionerFamilyName(UNKNOWN));
                        }
                    });

            if (agentKey.getOrganizationReference() != null) {
                messageContext.getInputBundleHolder()
                    .getResource(new IdType(agentKey.getOrganizationReference()))
                    .map(Organization.class::cast)
                    .map(OrganizationToAgentMapper::mapOrganizationToAgentInner)
                    .ifPresent(builder::organization);
            }
        } else if (agentKey.getOrganizationReference() != null) {
            messageContext.getInputBundleHolder()
                .getResource(new IdType(agentKey.getOrganizationReference()))
                .map(Organization.class::cast)
                .map(Organization::getName)
                .ifPresent(organizationName -> {
                    builder.practitioner(true);
                    builder.practitionerFamilyName(organizationName);
                });
        }

        buildPractitionerRole(agentKey).ifPresent(builder::practitionerRole);

        return TemplateUtils.fillTemplate(AGENT_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildPractitionerUnstructuredName(Practitioner practitioner) {
        if(practitioner.hasName() && practitioner.getNameFirstRep().hasText()) {
            return Optional.of(practitioner.getNameFirstRep().getText());
        }
        return Optional.empty();
    }

    private Optional<String> buildPractitionerRole(AgentDirectory.AgentKey agentKey) {
        Optional<PractitionerRole> practitionerRole = messageContext.getInputBundleHolder()
            .getPractitionerRoleFor(
                agentKey.getPractitionerReference(),
                agentKey.getOrganizationReference()
            );

        return practitionerRole
            .filter(AgentPersonMapper::hasPractitionerRoleDisplay)
            .map(PractitionerRole::getCodeFirstRep)
            .map(CodeableConcept::getCodingFirstRep)
            .map(Coding::getDisplay);
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
