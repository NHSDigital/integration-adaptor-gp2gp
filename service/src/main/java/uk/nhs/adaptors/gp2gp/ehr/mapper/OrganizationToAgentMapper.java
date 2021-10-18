package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AgentMapperTemplateParametersInner;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AgentMapperTemplateParametersManagingOrganization;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class OrganizationToAgentMapper {

    private static final Mustache AGENT_TEMPLATE_MANAGING_ORGANIZATION =
        TemplateUtils.loadTemplate("ehr_agent_template_managing_organization.mustache");
    private static final Mustache AGENT_TEMPLATE_INNER = TemplateUtils.loadTemplate("ehr_agent_template_inner.mustache");
    private static final Map<String, String> ADDRESS_USES = Map.of(
        "home", "H",
        "work", "WP",
        "temporary", "TMP"
    );

    public static String mapOrganizationToAgent(Organization organization, String newId) {
        var builder = AgentMapperTemplateParametersManagingOrganization.builder()
                .agentId(newId);

        if (organization.hasName()) {
            builder.name(organization.getName());
        }

        return TemplateUtils.fillTemplate(AGENT_TEMPLATE_MANAGING_ORGANIZATION, builder.build());
    }

    public static String mapOrganizationToAgentInner(Organization organization) {
        var builder = AgentMapperTemplateParametersInner.builder();

        buildName(organization).ifPresent(builder::agentName);
        buildTelecom(organization).ifPresent(builder::telecomValue);
        buildAddressUse(organization).ifPresent(builder::addressUse);
        var addressLine = buildAddressLine(organization);
        builder.addressLine(addressLine);
        var postalCode = buildPostalCode(organization);
        postalCode.ifPresent(builder::postalCode);

        if (!addressLine.isEmpty() || postalCode.isPresent()) {
            builder.addressPresent(true);
        }

        return TemplateUtils.fillTemplate(AGENT_TEMPLATE_INNER, builder.build());
    }

    private static Optional<String> buildName(Organization organization) {
        if (organization.hasName()) {
            return Optional.of(organization.getName());
        }
        return Optional.empty();
    }

    private static Optional<String> buildTelecom(Organization organization) {
        if (organization.hasTelecom()) {
            return organization.getTelecom()
                .stream()
                .filter(OrganizationToAgentMapper::checkIfWorkPhone)
                .map(ContactPoint::getValue)
                .findFirst();
        }
        return Optional.empty();
    }

    private static Optional<String> buildAddressUse(Organization organization) {
        if (organization.hasAddress() && organization.getAddressFirstRep().hasUse()) {
            var addressUse = organization.getAddressFirstRep().getUse().getDisplay();
            return Optional.of(ADDRESS_USES.getOrDefault(addressUse.toLowerCase(), StringUtils.EMPTY));
        }
        return Optional.empty();
    }

    private static List<String> buildAddressLine(Organization organization) {
        if (organization.hasAddress()) {
            var addressLines = organization.getAddressFirstRep()
                .getLine()
                .stream()
                .map(StringType::getValue)
                .collect(Collectors.toList());

            if (organization.getAddressFirstRep().hasCity()) {
                addressLines.add(organization.getAddressFirstRep().getCity());
            }

            return addressLines;
        }
        return Collections.emptyList();
    }

    private static Optional<String> buildPostalCode(Organization organization) {
        if (organization.hasAddress() && organization.getAddressFirstRep().hasPostalCode()) {
            return Optional.of(organization.getAddressFirstRep()
                .getPostalCode());
        }
        return Optional.empty();
    }

    private static boolean checkIfWorkPhone(ContactPoint contactPoint) {
        return  (contactPoint.getSystem().getDisplay().equalsIgnoreCase("phone")
            && contactPoint.getUse().getDisplay().equalsIgnoreCase("work"));
    }
}
