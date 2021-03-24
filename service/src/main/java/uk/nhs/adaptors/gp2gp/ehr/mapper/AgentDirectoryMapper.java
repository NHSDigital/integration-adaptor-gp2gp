package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ReferralRequest;
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

    private final Set<String> mappedOrganizationsAndPractitioner = new HashSet<>();
    private final PractitionerAgentPersonMapper practitionerAgentPersonMapper;
    private final OrganizationToAgentMapper organizationToAgentMapper;

    public String mapEHRFolderToAgentDirectory(Bundle bundle, String nhsNumber) {
        var builder = AgentDirectoryParameter.builder()
            .patientManagingOrganization(extractPatientManagingOrganization(bundle, nhsNumber))
            .observationOrganizations(prepareObservationAgentsList(bundle))
            .referralRequestsOrganizations(prepareReferralRequestAgentsList(bundle))
            .agentPersons(preparePractitionerRoleAgentsList(bundle));

        return TemplateUtils.fillTemplate(AGENT_DIRECTORY_STRUCTURE_TEMPLATE, builder.build());
    }

    private String extractPatientManagingOrganization(Bundle bundle, String nhsNumber) {
        return AgentDirectoryExtractor.extractPatientByNhsNumber(bundle, nhsNumber)
            .filter(Patient::hasManagingOrganization)
            .map(patient -> ResourceExtractor.extractResourceByReference(bundle,
                patient.getManagingOrganization().getReferenceElement()))
            .map(Optional::get)
            .map(Organization.class::cast)
            .map(organizationToAgentMapper::mapOrganizationToAgent)
            .orElseThrow(() -> new EhrMapperException("No patient or managing organization found in ehrFolder with NHS Number: "
                + nhsNumber));
    }

    private List<String> prepareObservationAgentsList(Bundle bundle) {
        List<Observation> observations = AgentDirectoryExtractor.extractObservationsWithPerformers(bundle);

        return observations.stream()
            .map(observation -> buildAgentPerson(observation, bundle))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }

    private List<String> prepareReferralRequestAgentsList(Bundle bundle) {
        List<ReferralRequest> referralRequests = AgentDirectoryExtractor.extractReferralRequestsWithRequester(bundle);

        return referralRequests.stream()
            .map(referralRequest -> buildAgentPerson(referralRequest, bundle))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }

    private List<String> preparePractitionerRoleAgentsList(Bundle bundle) {
        List<Practitioner> practitioners = AgentDirectoryExtractor.extractRemainingPractitioners(bundle);
        var mappingTriplets = AgentDirectoryExtractor.extractAgentData(bundle, practitioners);

        return mappingTriplets.stream()
            .map(agentData -> mapAgentPerson(agentData.getPractitioner(), agentData.getPractitionerRole(), agentData.getOrganization()))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }

    private String buildAgentPerson(Observation observation, Bundle bundle) {
        var practitioner = AgentDirectoryExtractor.extractObservationResource(observation, bundle, ResourceType.Practitioner);
        var organization = AgentDirectoryExtractor.extractObservationResource(observation, bundle, ResourceType.Organization);
        if (practitioner.isPresent() && organization.isPresent()) {
            return mapAgentPerson((Practitioner) practitioner.get(),
                null,
                (Organization) organization.get());
        } else if (practitioner.isPresent()) {
            var agentDataList = AgentDirectoryExtractor.extractAgentData(bundle, List.of((Practitioner) practitioner.get()))
                .stream()
                .map(agentData -> mapAgentPerson(agentData.getPractitioner(), agentData.getPractitionerRole(), agentData.getOrganization()))
                .filter(StringUtils::isNotEmpty)
                .findFirst();
            if (agentDataList.isPresent()) {
                return agentDataList.get();
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildAgentPerson(ReferralRequest referralRequest, Bundle bundle) {
        var practitioner = ResourceExtractor
            .extractResourceByReference(bundle, referralRequest.getRequester().getAgent().getReferenceElement());
        var organization = ResourceExtractor
            .extractResourceByReference(bundle, referralRequest.getRequester().getOnBehalfOf().getReferenceElement());
        if (practitioner.isPresent() && organization.isPresent()) {
            return mapAgentPerson(practitioner.map(Practitioner.class::cast).get(),
                null,
                organization.map(Organization.class::cast).get());

        } else if (practitioner.isPresent()) {
            var agentDataList = AgentDirectoryExtractor.extractAgentData(bundle, List.of(practitioner.map(Practitioner.class::cast).get()))
                .stream()
                .map(agentData -> mapAgentPerson(agentData.getPractitioner(), agentData.getPractitionerRole(), agentData.getOrganization()))
                .filter(StringUtils::isNotEmpty)
                .findFirst();

            if (agentDataList.isPresent()) {
                return agentDataList.get();
            }
        }
        return StringUtils.EMPTY;
    }

    private String mapAgentPerson(Practitioner practitioner, PractitionerRole practitionerRole, Organization organization) {
        if (!organizationPractitionerHasBeenMapped(practitioner, organization)) {
            addOrganizationPractitionerPairToMap(practitioner, organization);
            return practitionerAgentPersonMapper.mapPractitionerToAgentPerson(
                practitioner,
                Optional.ofNullable(practitionerRole),
                Optional.ofNullable(organization)
            );
        }
        return StringUtils.EMPTY;
    }

    private boolean organizationPractitionerHasBeenMapped(Practitioner practitioner, Organization organization) {
        var mappedId = practitioner.getIdElement().getIdPart();
        if (organization != null) {
            mappedId += "-" + organization.getIdElement().getIdPart();
        }
        return mappedOrganizationsAndPractitioner.contains(mappedId);
    }

    private void addOrganizationPractitionerPairToMap(Practitioner practitioner, Organization organization) {
        var mappedId = practitioner.getIdElement().getIdPart();
        if (organization != null) {
            mappedId += "-" + organization.getIdElement().getIdPart();
        }
        mappedOrganizationsAndPractitioner.add(mappedId);
    }
}
