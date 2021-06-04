package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AgentDirectoryParameter;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AgentDirectoryMapper {
    private static final Mustache AGENT_DIRECTORY_STRUCTURE_TEMPLATE = TemplateUtils.loadTemplate("ehr_agent_structure_template.mustache");

    private final MessageContext messageContext;
    private final AgentPersonMapper agentPersonMapper;

    public String mapEHRFolderToAgentDirectory(Bundle bundle, String nhsNumber) {
        var builder = AgentDirectoryParameter.builder();

        Set<Map.Entry<AgentDirectory.AgentKey, String>> agentDirectories = messageContext.getAgentDirectory().getEntries();

        Organization organization = extractPatientManagingOrganization(bundle, nhsNumber)
            .orElseThrow(() -> new EhrMapperException("The ASR bundle does not contain a Patient resource"
                + " with the correct identifier and managingOrganization"));

        builder.patientManagingOrganization(OrganizationToAgentMapper.mapOrganizationToAgent(organization, getAgentId(organization)));

        builder.agentPersons(
            agentDirectories.stream()
                .filter(agentDirectory -> !isPatientManagingOrganization(organization, agentDirectory))
                .map(this::mapAgentDirectory)
                .collect(Collectors.toList())
        );

        return TemplateUtils.fillTemplate(AGENT_DIRECTORY_STRUCTURE_TEMPLATE, builder.build());
    }

    private String getAgentId(Organization organization) {
        Reference reference = new Reference(ResourceType.Organization.name() + "/" + organization.getIdElement().getIdPart());
        return messageContext.getAgentDirectory().getAgentId(reference);
    }

    private String mapAgentDirectory(Map.Entry<AgentDirectory.AgentKey, String> agentDirectory) {
        return agentPersonMapper.mapAgentPerson(agentDirectory.getKey(), agentDirectory.getValue());
    }

    private boolean isPatientManagingOrganization(Organization patientManagingOrganization,
        Map.Entry<AgentDirectory.AgentKey, String> agentDirectory) {
        AgentDirectory.AgentKey organizationAgentDirectoryKey =
            new AgentDirectory.AgentKey.AgentKeyBuilder().organizationReference(
                ResourceType.Organization.name() + "/" + patientManagingOrganization.getIdElement().getIdPart()
            ).build();
        return agentDirectory.getKey().equals(organizationAgentDirectoryKey);
    }

    private Optional<Organization> extractPatientManagingOrganization(Bundle bundle, String nhsNumber) {
        return AgentDirectoryExtractor.extractPatientByNhsNumber(bundle, nhsNumber)
            .filter(Patient::hasManagingOrganization)
            .flatMap(patient -> ResourceExtractor.extractResourceByReference(bundle,
                patient.getManagingOrganization().getReferenceElement()))
            .map(Organization.class::cast);
    }
}
