package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
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
    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";

    private final PractitionerAgentPersonMapper practitionerAgentPersonMapper;
    private final OrganizationToAgentMapper organizationToAgentMapper;
    private final MessageContext messageContext;

    public String mapEHRFolderToAgentDirectory(Bundle bundle, String nhsNumber) {
        var builder = AgentDirectoryParameter.builder()
            .patientManagingOrganization(extractPatientManagingOrganization(bundle, nhsNumber))
            .observationManagingOrganization(prepareObservationAgentsList(bundle));

        return TemplateUtils.fillTemplate(AGENT_DIRECTORY_STRUCTURE_TEMPLATE, builder.build());
    }

    private List<String> prepareReferralRequestAgentsList(Bundle bundle) {
        List<ReferralRequest> referralRequests = extractReferralRequestsWithRequester(bundle);
        return List.of();
    }

    private List<ReferralRequest> extractReferralRequestsWithRequester(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().equals(ResourceType.ReferralRequest))
            .map(resource -> (ReferralRequest) resource)
            .filter(ReferralRequest::hasRequester)
            .collect(Collectors.toList());
    }

    private List<String> prepareObservationAgentsList(Bundle bundle) {
        List<Observation> observations = extractObservationsWithPerformers(bundle);

        return observations.stream()
            .map(observation -> buildAgentPerson(observation, bundle))
            .collect(Collectors.toList());
    }

    private String buildAgentPerson(Observation observation, Bundle bundle) {
        var practitioner = extractObservationResource(observation, bundle, ResourceType.Practitioner);
        var organization = extractObservationResource(observation, bundle, ResourceType.Observation);
        if (practitioner.isPresent() && organization.isPresent()) {
            return practitionerAgentPersonMapper
                .mapPractitionerToAgentPerson((Practitioner) practitioner.get(),
                    Optional.empty(),
                    organization.map(value -> (Organization) value));
        }
        return StringUtils.EMPTY;
    }

    private Optional<Resource> extractObservationResource(Observation observation, Bundle bundle, ResourceType resourceType) {
        var reference = observation.getPerformer()
            .stream()
            .filter(reference1 -> reference1.getReferenceElement().getResourceType().equals(resourceType.name()))
            .findFirst();
        return reference.flatMap(value -> ResourceExtractor.extractResourceByReference(bundle, value.getReferenceElement()));
    }

    private List<Observation> extractObservationsWithPerformers(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().equals(ResourceType.Observation))
            .map(resource -> (Observation) resource)
            .filter(Observation::hasPerformer)
            .filter(this::performerContainersOrgAndPractitioner)
            .collect(Collectors.toList());
    }

    private boolean performerContainersOrgAndPractitioner(Observation observation) {
        var performers = observation.getPerformer();
        return listHasPractitioner(performers) && listHasOrganization(performers);
    }

    private boolean listHasPractitioner(List<Reference> references) {
        return listHasResourceType(references, ResourceType.Practitioner);
    }

    private boolean listHasOrganization(List<Reference> references) {
        return listHasResourceType(references, ResourceType.Organization);
    }

    private boolean listHasResourceType(List<Reference> references, ResourceType resourceType) {
        return references.stream()
            .map(reference -> reference.getReferenceElement().getResourceType())
            .anyMatch(referenceResourceType -> referenceResourceType.equals(resourceType.name()));
    }

    private String extractPatientManagingOrganization(Bundle bundle, String nhsNumber) {
        Optional<Patient> patientOptional = extractPatientByNhsNumber(bundle, nhsNumber);
        if (patientOptional.isPresent() && patientOptional.get().hasManagingOrganization()) {
            var organization = ResourceExtractor.extractResourceByReference(bundle,
                patientOptional.get().getManagingOrganization().getReferenceElement());
            if (organization.isPresent()) {
                return organizationToAgentMapper.mapOrganizationToAgent((Organization) organization.get());
            }
        }
        throw new EhrMapperException("No patient or managing organization found in ehrFolder with NHS Number: " + nhsNumber);
    }

    private Optional<Patient> extractPatientByNhsNumber(Bundle bundle, String nhsNumber) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(this::isPatientResource)
            .map(resource -> (Patient) resource)
            .filter(patient -> isNhsNumberMatching(patient, nhsNumber))
            .findFirst();
    }

    private boolean isPatientResource(Resource resource) {
        return resource.getResourceType().equals(ResourceType.Patient);
    }

    private boolean isNhsNumberMatching(Patient patient, String nhsNumber) {
        return patient.getIdentifier()
            .stream()
            .filter(identifier -> identifier.getSystem().equals(NHS_NUMBER_SYSTEM))
            .anyMatch(identifier -> identifier.getValue().equals(nhsNumber));
    }
}
