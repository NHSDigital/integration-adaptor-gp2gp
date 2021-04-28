package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.BaseReference;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import lombok.extern.slf4j.Slf4j;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class AgentDirectoryExtractor {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final List<ResourceType> REMAINING_RESOURCE_TYPES = List.of(
        ResourceType.Immunization,
        ResourceType.Encounter,
        ResourceType.ReferralRequest,
        ResourceType.AllergyIntolerance,
        ResourceType.MedicationRequest,
        ResourceType.Condition);
    private static final Map<ResourceType, Predicate<Resource>> RESOURCE_HAS_PRACTITIONER = Map.of(
        ResourceType.Immunization, resource -> ((Immunization) resource).hasPractitioner(),
        ResourceType.Encounter, resource -> ((Encounter) resource).hasParticipant()
            && ((Encounter) resource).getParticipantFirstRep().hasIndividual(),
        ResourceType.ReferralRequest, AgentDirectoryExtractor::referralRequestHasPractitionerRequesterAgent,
        ResourceType.AllergyIntolerance, resource -> ((AllergyIntolerance) resource).hasAsserter()
            || ((AllergyIntolerance) resource).hasRecorder(),
        ResourceType.MedicationRequest, resource -> ((MedicationRequest) resource).hasRecorder(),
        ResourceType.Condition, resource -> ((Condition) resource).hasAsserter()
    );
    private static final Map<ResourceType, Function<Resource, IIdType>> RESOURCE_EXTRACT_IIDTYPE = Map.of(
        ResourceType.Immunization, resource -> ((Immunization) resource).getPractitionerFirstRep().getActor().getReferenceElement(),
        ResourceType.Encounter, resource -> ((Encounter) resource).getParticipantFirstRep().getIndividual().getReferenceElement(),
        ResourceType.ReferralRequest, resource -> ((ReferralRequest) resource).getRequester().getAgent().getReferenceElement(),
        ResourceType.AllergyIntolerance, AgentDirectoryExtractor::extractIIdTypeFromAllergyIntolerance,
        ResourceType.MedicationRequest, resource -> ((MedicationRequest) resource).getRecorder().getReferenceElement(),
        ResourceType.Condition, resource -> ((Condition) resource).getAsserter().getReferenceElement()
    );

    public static List<ReferralRequest> extractReferralRequestsWithRequester(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, ReferralRequest.class)
            .filter(ReferralRequest::hasRequester)
            .collect(Collectors.toList());
    }

    public static List<Observation> extractObservationsWithPerformers(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, Observation.class)
            .filter(Observation::hasPerformer)
            .collect(Collectors.toList());
    }

    public static Optional<Patient> extractPatientByNhsNumber(Bundle bundle, String nhsNumber) {
        return ResourceExtractor.extractResourcesByType(bundle, Patient.class)
            .filter(patient -> isNhsNumberMatching(patient, nhsNumber))
            .findFirst();
    }

    public static List<Practitioner> extractRemainingPractitioners(Bundle bundle) {
        return bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> REMAINING_RESOURCE_TYPES.contains(resource.getResourceType()))
            .filter(AgentDirectoryExtractor::containsRelevantPractitioner)
            .peek(resource -> LOGGER.debug("{} contains a relevant practitioner reference", resource.getId()))
            .map(AgentDirectoryExtractor::extractIIdTypes)
            .peek(reference -> LOGGER.debug("Extracted reference to {}", reference))
            .map(reference -> ResourceExtractor.extractResourceByReference(bundle, reference))
            .flatMap(Optional::stream)
            .map(resource -> getPractitionerIfPractitionerRole(resource, bundle))
            .map(Practitioner.class::cast)
            .distinct()
            .collect(Collectors.toList());
    }

    // TODO: workaround for NIAD-1300, remove once PractitionerRole references not used in GPC demonstrator
    private static Resource getPractitionerIfPractitionerRole(Resource resource, Bundle bundle) {
        if (resource.getResourceType().equals(ResourceType.PractitionerRole)) {
            var practitionerReference = ((PractitionerRole) resource).getPractitioner();
            LOGGER.warn("Encountered a PractitionerRole {} where Practitioner was expected."
                + "Swapping PractitionerRole for its Practitioner {}. The related organisation may "
                + "be mapped incorrectly", resource.getId(), practitionerReference.getReference());
            return ResourceExtractor.extractResourceByReference(bundle, practitionerReference.getReferenceElement())
                .orElseThrow(() -> new EhrMapperException("Unable to locate resource " + practitionerReference.getReference()
                    + " in the bundle"));
        }
        return resource;
    }

    public static List<AgentData> extractAgentData(Bundle bundle, List<Practitioner> practitioners) {

        List<PractitionerRole> practitionerRoles = ResourceExtractor.extractResourcesByType(bundle, PractitionerRole.class)
            .filter(practitionerRole -> referencesPractitioner(practitionerRole, practitioners))
            .collect(Collectors.toList());

        List<Organization> organizations = practitionerRoles.stream()
            .map(PractitionerRole::getOrganization)
            .map(BaseReference::getReferenceElement)
            .map(reference -> ResourceExtractor.extractResourceByReference(bundle, reference))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Organization.class::cast)
            .collect(Collectors.toList());

        return practitioners.stream()
            .map(practitioner -> createAgentData(practitioner, practitionerRoles, organizations))
            .collect(Collectors.toList());
    }

    private static AgentData createAgentData(Practitioner practitioner,
        List<PractitionerRole> practitionerRoles, List<Organization> organizations) {

        var practitionerRole = extractRelevantPractitionerRole(practitioner, practitionerRoles);
        if (practitionerRole.isPresent()) {
            var organization = extractRelevantOrganization(practitionerRole.get(), organizations);
            if (organization.isPresent()) {
                return new AgentData(practitioner, practitionerRole.get(), organization.get());
            }
        }
        return new AgentData(practitioner, null, null);
    }

    public static Optional<Resource> extractObservationResource(Observation observation, Bundle bundle, ResourceType resourceType) {
        var reference = observation.getPerformer()
            .stream()
            .map(BaseReference::getReferenceElement)
            .filter(reference1 -> reference1.getResourceType().equals(resourceType.name()))
            .findFirst();
        return reference.flatMap(value -> ResourceExtractor.extractResourceByReference(bundle, value));
    }

    private static boolean referencesPractitioner(PractitionerRole practitionerRole, List<Practitioner> practitioners) {
        var practitionerRolePractitionerId = practitionerRole.getPractitioner().getReferenceElement().getIdPart();
        return practitioners.stream()
            .map(Practitioner::getIdElement)
            .map(IdType::getIdPart)
            .anyMatch(practitionerRolePractitionerId::equals);
    }

    private static Optional<PractitionerRole> extractRelevantPractitionerRole(Practitioner practitioner,
        List<PractitionerRole> practitionerRoles) {
        var practitionerId = practitioner.getIdElement().getIdPart();
        return practitionerRoles
            .stream()
            .filter(practitionerRole -> practitionerRole.getPractitioner().getReferenceElement().getIdPart().equals(practitionerId))
            .findFirst();
    }

    private static Optional<Organization> extractRelevantOrganization(PractitionerRole practitionerRole, List<Organization> organizations) {
        var organizationId = practitionerRole.getOrganization().getReferenceElement().getIdPart();
        return organizations
            .stream()
            .filter(organization -> organization.getIdElement().getIdPart().equals(organizationId))
            .findFirst();
    }

    private static boolean containsRelevantPractitioner(Resource resource) {
        Predicate<Resource> containsPractitioner = RESOURCE_HAS_PRACTITIONER.getOrDefault(resource.getResourceType(), (resource1) -> false);
        return containsPractitioner.test(resource);
    }

    private static IIdType extractIIdTypes(Resource resource) {
        Function<Resource, IIdType> extractor = RESOURCE_EXTRACT_IIDTYPE.get(resource.getResourceType());
        return extractor.apply(resource);
    }

    private static boolean isNhsNumberMatching(Patient patient, String nhsNumber) {
        return patient.getIdentifier()
            .stream()
            .filter(identifier -> identifier.getSystem().equals(NHS_NUMBER_SYSTEM))
            .anyMatch(identifier -> identifier.getValue().equals(nhsNumber));
    }

    private static IIdType extractIIdTypeFromAllergyIntolerance(Resource resource) {
        Optional<AllergyIntolerance> allergyIntolerance = Optional.of(resource)
            .map(AllergyIntolerance.class::cast);

        return allergyIntolerance
            .map(AllergyIntolerance::getAsserter)
            .map(BaseReference::getReferenceElement)
            .filter(IIdType::hasIdPart)
            .orElseGet(() -> allergyIntolerance
                .map(AllergyIntolerance::getRecorder)
                .map(BaseReference::getReferenceElement).get());
    }

    private static boolean referralRequestHasPractitionerRequesterAgent(Resource resource) {
        ReferralRequest referralRequest = (ReferralRequest) resource;
        return referralRequest.hasRequester()
            && referralRequest.getRequester().getAgent().getReferenceElement()
            .getResourceType().equals(ResourceType.Practitioner.name());
    }

    @Data
    public static class AgentData {
        private final Practitioner practitioner;
        private final PractitionerRole practitionerRole;
        private final Organization organization;
    }
}
