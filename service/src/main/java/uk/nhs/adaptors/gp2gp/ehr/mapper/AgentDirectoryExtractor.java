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
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

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

    public static Optional<Patient> extractPatientByNhsNumber(Bundle bundle, String nhsNumber) {
        return ResourceExtractor.extractResourcesByType(bundle, Patient.class)
            .filter(patient -> isNhsNumberMatching(patient, nhsNumber))
            .findFirst();
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
