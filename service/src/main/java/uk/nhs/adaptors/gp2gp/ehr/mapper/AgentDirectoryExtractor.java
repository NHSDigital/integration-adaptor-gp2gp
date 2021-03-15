package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.sql.Ref;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

public class AgentDirectoryExtractor {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final List<ResourceType> REMAINING_RESOURCE_TYPES = List.of(ResourceType.Immunization,
            ResourceType.Encounter,
            ResourceType.ReferralRequest,
            ResourceType.AllergyIntolerance,
            ResourceType.Condition);
    private static final Map<ResourceType, Predicate<Resource>> RESOURCE_HAS_PRACTITIONER = Map.of(
        ResourceType.Immunization, (resource) -> ((Immunization) resource).hasPractitioner(),
        ResourceType.Encounter, (resource -> ((Encounter) resource).hasParticipant() && ((Encounter) resource).getParticipantFirstRep().hasIndividual()),
        ResourceType.ReferralRequest, (resource -> ((ReferralRequest) resource).hasRecipient()),
        ResourceType.AllergyIntolerance, (resource -> ((AllergyIntolerance) resource).hasAsserter() || ((AllergyIntolerance) resource).hasRecorder()),
        ResourceType.Condition, (resource -> ((Condition) resource).hasAsserter())
    );
    private static final Map<ResourceType, Function<Resource, IIdType>> RESOURCE_EXTRACT_IIDTYPE = Map.of(
        ResourceType.Immunization, (resource -> ((Immunization) resource).getPractitionerFirstRep().getActor().getReferenceElement()),
        ResourceType.Encounter, (resource -> ((Encounter) resource).getParticipantFirstRep().getIndividual().getReferenceElement()),
        ResourceType.ReferralRequest, (resource -> ((ReferralRequest) resource).getRecipientFirstRep().getReferenceElement()),
        ResourceType.AllergyIntolerance, AgentDirectoryExtractor::extractIIdTypeFromAllergyIntolerance,
        ResourceType.Condition, (resource -> ((Condition) resource).getAsserter().getReferenceElement())
    );

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

    public static List<Practitioner> extractRemainingPractitioners(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> REMAINING_RESOURCE_TYPES.contains(resource.getResourceType()))
            .filter(AgentDirectoryExtractor::containsRelevantPractitioner)
            .map(AgentDirectoryExtractor::extractIIdTypes)
            .map(reference -> ResourceExtractor.extractResourceByReference(bundle, reference))
            .filter(Optional::isPresent)
            .map(resource -> (Practitioner) resource.get())
            .distinct()
            .collect(Collectors.toList());
    }

    public static List<Triple<Practitioner, Optional<PractitionerRole>, Optional<Organization>>> extractPractitionerRoleTriples(Bundle bundle, List<Practitioner> practitioners) {
        List<PractitionerRole> practitionerRoles = bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().equals(ResourceType.PractitionerRole))
            .map(resource -> (PractitionerRole) resource)
            .filter(practitionerRole -> referencesPractitioner(practitionerRole, practitioners))
            .collect(Collectors.toList());

        List<Organization> organizations = practitionerRoles.stream()
            .map(practitionerRole -> practitionerRole.getOrganization().getReferenceElement())
            .map(reference -> ResourceExtractor.extractResourceByReference(bundle, reference))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(resource -> (Organization) resource)
            .collect(Collectors.toList());

        return List.of();
    }

    private static boolean referencesPractitioner(PractitionerRole practitionerRole, List<Practitioner> practitioners) {
        var practitionerRolePractitionerId = practitionerRole.getPractitioner().getReferenceElement().getIdPart();
        return practitioners.stream()
            .anyMatch(practitioner -> practitioner.getIdElement().getIdPart().equals(practitionerRolePractitionerId));
    }

    public static Optional<Resource> extractObservationResource(Observation observation, Bundle bundle, ResourceType resourceType) {
        var reference = observation.getPerformer()
            .stream()
            .filter(reference1 -> reference1.getReferenceElement().getResourceType().equals(resourceType.name()))
            .findFirst();
        return reference.flatMap(value -> ResourceExtractor.extractResourceByReference(bundle, value.getReferenceElement()));
    }

    private static boolean containsRelevantPractitioner(Resource resource) {
        Predicate<Resource> containsPractitioner = RESOURCE_HAS_PRACTITIONER.getOrDefault(resource.getResourceType(), (resource1) -> false);
        return containsPractitioner.test(resource);
    }

    private static IIdType extractIIdTypes(Resource resource) {
        Function<Resource, IIdType> extractor = RESOURCE_EXTRACT_IIDTYPE.get(resource.getResourceType());
        return extractor.apply(resource);
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

    private static IIdType extractIIdTypeFromAllergyIntolerance(Resource resource) {
        Optional<IIdType> practitionerReference = Optional.of(((AllergyIntolerance) resource).getAsserter().getReferenceElement());
        return practitionerReference.orElseGet(() -> ((AllergyIntolerance) resource).getRecorder().getReferenceElement());
    }
}
