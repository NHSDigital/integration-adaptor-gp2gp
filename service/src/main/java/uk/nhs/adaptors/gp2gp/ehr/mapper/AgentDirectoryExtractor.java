package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;

import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

public class AgentDirectoryExtractor {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";

    public static List<ReferralRequest> extractReferralRequestsWithRequester(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().equals(ResourceType.ReferralRequest))
            .map(resource -> (ReferralRequest) resource)
            .filter(ReferralRequest::hasRequester)
            .filter(AgentDirectoryExtractor::requesterContainsAgentAndOrganization)
            .collect(Collectors.toList());
    }

    public static List<Observation> extractObservationsWithPerformers(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().equals(ResourceType.Observation))
            .map(resource -> (Observation) resource)
            .filter(Observation::hasPerformer)
            .filter(AgentDirectoryExtractor::performerContainersOrgAndPractitioner)
            .collect(Collectors.toList());
    }

    public static Optional<Patient> extractPatientByNhsNumber(Bundle bundle, String nhsNumber) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(AgentDirectoryExtractor::isPatientResource)
            .map(resource -> (Patient) resource)
            .filter(patient -> isNhsNumberMatching(patient, nhsNumber))
            .findFirst();
    }

    public static Optional<Resource> extractObservationResource(Observation observation, Bundle bundle, ResourceType resourceType) {
        var reference = observation.getPerformer()
            .stream()
            .filter(reference1 -> reference1.getReferenceElement().getResourceType().equals(resourceType.name()))
            .findFirst();
        return reference.flatMap(value -> ResourceExtractor.extractResourceByReference(bundle, value.getReferenceElement()));
    }

    private static boolean performerContainersOrgAndPractitioner(Observation observation) {
        var performers = observation.getPerformer();
        return listHasPractitioner(performers) && listHasOrganization(performers);
    }

    private static boolean listHasPractitioner(List<Reference> references) {
        return listHasResourceType(references, ResourceType.Practitioner);
    }

    private static boolean listHasOrganization(List<Reference> references) {
        return listHasResourceType(references, ResourceType.Organization);
    }

    private static boolean listHasResourceType(List<Reference> references, ResourceType resourceType) {
        return references.stream()
            .map(reference -> reference.getReferenceElement().getResourceType())
            .anyMatch(referenceResourceType -> referenceResourceType.equals(resourceType.name()));
    }

    private static boolean requesterContainsAgentAndOrganization(ReferralRequest referralRequest) {
        var requester = referralRequest.getRequester();
        return requesterAgentContainsPractitioner(requester) && requester.hasOnBehalfOf();
    }

    private static boolean requesterAgentContainsPractitioner(ReferralRequest.ReferralRequestRequesterComponent requester) {
        return requester.getAgent().getReferenceElement().getResourceType().equals(ResourceType.Practitioner.name());
    }

    private static boolean isPatientResource(Resource resource) {
        return resource.getResourceType().equals(ResourceType.Patient);
    }

    private static boolean isNhsNumberMatching(Patient patient, String nhsNumber) {
        return patient.getIdentifier()
            .stream()
            .filter(identifier -> identifier.getSystem().equals(NHS_NUMBER_SYSTEM))
            .anyMatch(identifier -> identifier.getValue().equals(nhsNumber));
    }
}
