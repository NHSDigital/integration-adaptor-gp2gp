package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static java.util.function.Predicate.not;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.MedicationRequestUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EncounterComponentsMapper {

    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final String TOPIC_LIST_CODE = "25851000000105";
    private static final String CATEGORY_LIST_CODE = "24781000000107";
    private static final List<String> COMPONENTS_LISTS = Arrays.asList(TOPIC_LIST_CODE, CATEGORY_LIST_CODE);

    private static final String BLOOD_PRESSURE_READING_CODE = "163020007";
    private static final String ARTERIAL_BLOOD_PRESSURE_CODE = "386534000";
    private static final String BLOOD_PRESSURE_CODE = "75367002";
    private static final String STANDING_BLOOD_PRESSURE_CODE = "163034007";
    private static final String SITTING_BLOOD_PRESSURE_CODE = "163035008";
    private static final String LAYING_BLOOD_PRESSURE_CODE = "163033001";
    private static final String NOT_IMPLEMENTED_MAPPER_PLACE_HOLDER = "<!-- %s/%s -->";
    private static final boolean IS_NESTED = false;

    private final Map<ResourceType, Function<Resource, Optional<String>>> encounterComponents = Map.of(
        ResourceType.AllergyIntolerance, this::mapAllergyIntolerance,
        ResourceType.Condition, this::mapCondition,
        ResourceType.DocumentReference, this::mapDocumentReference,
        ResourceType.Immunization, this::mapImmunization,
        ResourceType.List, this::mapListResource,
        ResourceType.MedicationRequest, this::mapMedicationRequest,
        ResourceType.Observation, this::mapObservation,
        ResourceType.ProcedureRequest, this::mapProcedureRequest,
        ResourceType.ReferralRequest, this::mapReferralRequest,
        ResourceType.DiagnosticReport, this::mapDiagnosticReport);

    private final MessageContext messageContext;

    private final AllergyStructureMapper allergyStructureMapper;
    private final BloodPressureMapper bloodPressureMapper;
    private final ConditionLinkSetMapper conditionLinkSetMapper;
    private final DiaryPlanStatementMapper diaryPlanStatementMapper;
    private final DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper;
    private final ImmunizationObservationStatementMapper immunizationObservationStatementMapper;
    private final MedicationStatementMapper medicationStatementMapper;
    private final ObservationToNarrativeStatementMapper observationToNarrativeStatementMapper;
    private final ObservationStatementMapper observationStatementMapper;
    private final RequestStatementMapper requestStatementMapper;
    private final DiagnosticReportMapper diagnosticReportMapper;

    public static final List<String> BLOOD_CODES = List.of(BLOOD_PRESSURE_READING_CODE, ARTERIAL_BLOOD_PRESSURE_CODE,
        BLOOD_PRESSURE_CODE, STANDING_BLOOD_PRESSURE_CODE, SITTING_BLOOD_PRESSURE_CODE, LAYING_BLOOD_PRESSURE_CODE);
    public static final String NARRATIVE_STATEMENT_CODE = "37331000000100";

    private static final String NIAD_1409_INVALID_REFERENCE = "Referral items are not supported by the provider system";

    public String mapComponents(Encounter encounter) {
        Optional<ListResource> listReferencedToEncounter =
            messageContext.getInputBundleHolder().getListReferencedToEncounter(encounter.getIdElement(), CONSULTATION_LIST_CODE);

        return listReferencedToEncounter
            .map(this::mapListResourceToComponents)
            .orElse(StringUtils.EMPTY);
    }

    public Optional<String> mapResourceToComponent(Resource resource) {
        return encounterComponents.getOrDefault(resource.getResourceType(), this::mapDefaultNotImplemented)
            .apply(resource);
    }

    private String mapListResourceToComponents(ListResource listReferencedToEncounter) {
        LOGGER.debug("Mapping List {} that contains {} entries", listReferencedToEncounter.getId(),
            listReferencedToEncounter.getEntry().size());
        return listReferencedToEncounter.getEntry()
            .stream()
            .map(this::mapItemToComponent)
            .flatMap(Optional::stream)
            .collect(Collectors.joining());
    }

    private Optional<String> mapItemToComponent(ListResource.ListEntryComponent item) {
        final String referenceValue = item.getItem().getReference();
        LOGGER.debug("Processing list item {}", referenceValue);

        // TODO: workaround for NIAD-1409 where text appears instead of a resource reference
        if (referenceValue == null || NIAD_1409_INVALID_REFERENCE.equals(referenceValue)) {
            LOGGER.warn("Detected an invalid reference in the GP Connect Demonstrator dataset. "
                + "Skipping resource with reference=\"{}\" display=\"{}\"", referenceValue,
                item.getItem().getDisplay());
            return Optional.empty();
        }

        Resource resource = messageContext.getInputBundleHolder().getRequiredResource(item.getItem().getReferenceElement());
        LOGGER.debug("Translating list entry resource {}", resource.getId());
        if (encounterComponents.containsKey(resource.getResourceType())) {
            return encounterComponents.get(resource.getResourceType()).apply(resource);
        } else {
            throw new EhrMapperException("Unsupported resource in consultation list: " + resource.getId());
        }
    }

    private Optional<String> mapDefaultNotImplemented(Resource resource) {
        return Optional.of(String.format(NOT_IMPLEMENTED_MAPPER_PLACE_HOLDER,
            resource.getIdElement().getResourceType(),
            resource.getIdElement().getIdPart()));
    }

    private Optional<String> mapAllergyIntolerance(Resource resource) {
        return Optional.of(allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure((AllergyIntolerance) resource));
    }

    private Optional<String> mapCondition(Resource resource) {
        return Optional.of(conditionLinkSetMapper.mapConditionToLinkSet((Condition) resource, IS_NESTED));
    }

    private Optional<String> mapDocumentReference(Resource resource) {
        return Optional.of(
            documentReferenceToNarrativeStatementMapper.mapDocumentReferenceToNarrativeStatement((DocumentReference) resource));
    }

    private Optional<String> mapImmunization(Resource resource) {
        return Optional.of(
            immunizationObservationStatementMapper.mapImmunizationToObservationStatement((Immunization) resource, IS_NESTED));
    }

    private Optional<String> mapListResource(Resource resource) {
        ListResource listResource = (ListResource) resource;

        if (listResource.hasEntry() && CodeableConceptMappingUtils.hasCode(listResource.getCode(), COMPONENTS_LISTS)) {
            return Optional.of(mapListResourceToComponents(listResource));
        }

        return Optional.empty();
    }

    private Optional<String> mapMedicationRequest(Resource resource) {
        return Optional.of(resource)
            .map(MedicationRequest.class::cast)
            .filter(not(MedicationRequestUtils::isMedicationRequestSuppressed))
            .map(medicationStatementMapper::mapMedicationRequestToMedicationStatement);
    }

    private Optional<String> mapObservation(Resource resource) {
        Observation observation = (Observation) resource;
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(NARRATIVE_STATEMENT_CODE))) {
            return Optional.of(observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(observation, IS_NESTED));
        }
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), BLOOD_CODES)) {
            return Optional.of(bloodPressureMapper.mapBloodPressure(observation, IS_NESTED));
        }

        return Optional.of(observationStatementMapper.mapObservationToObservationStatement(observation, IS_NESTED));
    }

    private Optional<String> mapProcedureRequest(Resource resource) {
        return Optional.of(diaryPlanStatementMapper.mapDiaryProcedureRequestToPlanStatement((ProcedureRequest) resource, IS_NESTED));
    }

    private Optional<String> mapReferralRequest(Resource resource) {
        return Optional.of(requestStatementMapper.mapReferralRequestToRequestStatement((ReferralRequest) resource, IS_NESTED));
    }

    private Optional<String> mapDiagnosticReport(Resource resource) {
        return Optional.of(diagnosticReportMapper.mapDiagnosticReportToCompoundStatement((DiagnosticReport) resource));
    }
}
