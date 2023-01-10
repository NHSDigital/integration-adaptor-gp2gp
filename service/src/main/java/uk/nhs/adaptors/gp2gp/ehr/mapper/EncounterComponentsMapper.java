package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static java.util.function.Predicate.not;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.CompoundStatementClassCode.CATEGORY;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.CompoundStatementClassCode.TOPIC;
import static uk.nhs.adaptors.gp2gp.ehr.utils.IgnoredResourcesUtils.isIgnoredResourceType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.CompoundStatementParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.MedicationRequestUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EncounterComponentsMapper {

    public static final String NARRATIVE_STATEMENT_CODE = "37331000000100";
    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final String TOPIC_LIST_CODE = "25851000000105";
    private static final String CATEGORY_LIST_CODE = "24781000000107";
    private static final String NOT_IMPLEMENTED_MAPPER_PLACE_HOLDER = "<!-- %s/%s -->";
    private static final boolean IS_NESTED = false;
    private static final String NIAD_1409_INVALID_REFERENCE = "Referral items are not supported by the provider system";
    private static final Mustache COMPOUND_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_compound_statement_template.mustache");
    private static final String RELATED_PROBLEM_EXTENSION_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-RelatedProblemHeader-1";
    private static final String RELATED_PROBLEM_TARGET = "target";
    private static final String COMPLETE_CODE = "COMPLETE";

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
    private final BloodPressureValidator bloodPressureValidator;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final Map<ResourceType, Function<Resource, Optional<String>>> encounterComponents = Map.of(
        ResourceType.AllergyIntolerance, this::mapAllergyIntolerance,
        ResourceType.Condition, this::mapCondition,
        ResourceType.DocumentReference, this::mapDocumentReference,
        ResourceType.Immunization, this::mapImmunization,
        ResourceType.MedicationRequest, this::mapMedicationRequest,
        ResourceType.Observation, this::mapObservation,
        ResourceType.ProcedureRequest, this::mapProcedureRequest,
        ResourceType.ReferralRequest, this::mapReferralRequest,
        ResourceType.DiagnosticReport, this::mapDiagnosticReport);

    public String mapComponents(Encounter encounter) {
        Optional<ListResource> listReferencedToEncounter =
            messageContext.getInputBundleHolder().getListReferencedToEncounter(encounter.getIdElement(), CONSULTATION_LIST_CODE);

        if (listReferencedToEncounter.isEmpty()) {
            return StringUtils.EMPTY;
        }

        var entries = listReferencedToEncounter.orElseThrow().getEntry();
        List<ListResource> topics = entries.stream()
            .map(entry -> entry.getItem().getReferenceElement())
            .map(reference ->
                (ListResource) messageContext
                    .getInputBundleHolder()
                    .getRequiredResource(reference))
            .collect(Collectors.toList());

        return topics.stream()
            .map(this::mapTopicListToComponent)
            .collect(Collectors.joining());
    }

    private String mapTopicListToComponent(ListResource topicList) {

        if (!CodeableConceptMappingUtils.hasCode(topicList.getCode(), List.of(TOPIC_LIST_CODE))) {
            throw new EhrMapperException(String.format("Unexpected list %s referenced in Consultation, expected list to be coded as " +
                "Topic (EHR)", topicList.getId()));
        }

        String components = mapTopicListComponents(topicList);

        if(StringUtils.isAllEmpty(components)) {
            return components;
        }

        String effectiveTime = prepareEffectiveTime(topicList);
        String availabilityTime = prepareAvailabilityTime(topicList);

        var params = CompoundStatementParameters.builder()
            .nested(false)
            .id(messageContext.getIdMapper().getOrNew(ResourceType.List, topicList.getIdElement()))
            .classCode(TOPIC.getCode())
            .compoundStatementCode(prepareCdForTopic(topicList))
            .statusCode(COMPLETE_CODE)
            .effectiveTime(effectiveTime)
            .availabilityTime(availabilityTime)
            .components(components)
            .build();

        return TemplateUtils.fillTemplate(COMPOUND_STATEMENT_TEMPLATE, params);
    }

    private String mapTopicListComponents(ListResource topicList) {

        String categories = topicList.getEntry().stream()
            .map(entry -> entry.getItem().getReferenceElement())
            .map(reference -> messageContext
                .getInputBundleHolder()
                .getRequiredResource(reference))
            .filter(resource -> resource.getResourceType().equals(ResourceType.List))
            .map(resource -> (ListResource) resource)
            .map(this::mapCategoryListToComponent)
            .collect(Collectors.joining());

        String uncategorisedComponents = mapListResourceToComponents(topicList);

        return categories + uncategorisedComponents;
    }

    private String mapCategoryListToComponent(ListResource categoryList) {

        if (!CodeableConceptMappingUtils.hasCode(categoryList.getCode(), List.of(CATEGORY_LIST_CODE))) {
            throw new EhrMapperException(String.format("Unexpected list %s referenced in Topic (EHR), expected list to be coded as " +
                "Category (EHR)", categoryList.getId()));
        }

            String components = mapListResourceToComponents(categoryList);

            if (StringUtils.isAllEmpty(components)) {
                return components;
            }

            String effectiveTime = prepareEffectiveTime(categoryList);
            String availabilityTime = prepareAvailabilityTime(categoryList);

            var params = CompoundStatementParameters.builder()
                .nested(true)
                .id(messageContext.getIdMapper().getOrNew(ResourceType.List, categoryList.getIdElement()))
                .classCode(CATEGORY.getCode())
                .compoundStatementCode(codeableConceptCdMapper.mapCodeableConceptToCd(categoryList.getCode()))
                .statusCode(COMPLETE_CODE)
                .effectiveTime(effectiveTime)
                .availabilityTime(availabilityTime)
                .components(components)
                .build();

            return TemplateUtils.fillTemplate(COMPOUND_STATEMENT_TEMPLATE, params);
    }

    private String mapListResourceToComponents(ListResource listResource) {
        LOGGER.debug("Mapping List {} that contains {} entries", listResource.getId(),
            listResource.getEntry().size());
        return listResource.getEntry()
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
        } else if (isIgnoredResourceType(resource.getResourceType()) || resource.getResourceType().equals(ResourceType.List)) {
            // lists referenced within consultations are only mapped as topics or categories
            // so should be ignored when mapping individual items

            if (!resource.getResourceType().equals(ResourceType.List)) {
                LOGGER.info(String.format("Resource of type: %s has been ignored", resource.getResourceType()));
            }

            return Optional.empty();
        }
        else {
            throw new EhrMapperException("Unsupported resource in consultation list: " + resource.getId());
        }
    }

    public Optional<String> mapResourceToComponent(Resource resource) {
        return encounterComponents.getOrDefault(resource.getResourceType(), this::mapDefaultNotImplemented)
            .apply(resource);
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
        if (bloodPressureValidator.isValidBloodPressure(observation)) {
            return Optional.of(bloodPressureMapper.mapBloodPressure(observation, IS_NESTED));
        }

        return Optional.of(observationStatementMapper.mapObservationToObservationStatement(observation, IS_NESTED));
    }

    private Optional<String> mapProcedureRequest(Resource resource) {
        return Optional.ofNullable(
            diaryPlanStatementMapper.mapDiaryProcedureRequestToPlanStatement((ProcedureRequest) resource, IS_NESTED)
        );
    }

    private Optional<String> mapReferralRequest(Resource resource) {
        return Optional.of(requestStatementMapper.mapReferralRequestToRequestStatement((ReferralRequest) resource, IS_NESTED));
    }

    private Optional<String> mapDiagnosticReport(Resource resource) {
        return Optional.of(diagnosticReportMapper.mapDiagnosticReportToCompoundStatement((DiagnosticReport) resource));
    }

    private String prepareCdForTopic(ListResource topicList) {
        var extensions = topicList.getExtension();

        Optional<Reference> conditionRef = extensions.stream()
            .filter(ext -> ext.getUrl().equals(RELATED_PROBLEM_EXTENSION_URL))
            .flatMap(ext -> ext.getExtension().stream())
            .filter(relatedProblemExt -> relatedProblemExt.getUrl().equals(RELATED_PROBLEM_TARGET))
            .findFirst()
            .map(ext -> (Reference) ext.getValue());

        Optional<CodeableConcept> relatedProblem = conditionRef
            .map(reference -> (Condition) messageContext
                .getInputBundleHolder()
                .getRequiredResource(reference.getReferenceElement()))
            .map(Condition::getCode);

        Optional<String> title = Optional.ofNullable(topicList.getTitle());

        if (relatedProblem.isPresent() && title.isPresent()) {
            return codeableConceptCdMapper.mapCdForTopic(relatedProblem.orElseThrow(), title.orElseThrow());
        }

        if (relatedProblem.isPresent()) {
            return codeableConceptCdMapper.mapCdForTopic(relatedProblem.orElseThrow());
        }

        if (title.isPresent()) {
            return codeableConceptCdMapper.mapCdForTopic(title.orElseThrow());
        }

        return codeableConceptCdMapper.mapCdForTopic();
    }

    private String prepareEffectiveTime(ListResource listResource) {
        return StatementTimeMappingUtils.prepareEffectiveTimeForEncounter(findEncounterForList(listResource));
    }

    private String prepareAvailabilityTime(ListResource listResource) {
        var amendedDateTime = listResource.getDateElement();

        if (amendedDateTime.isEmpty()) {
            var encounter = findEncounterForList(listResource);
            return StatementTimeMappingUtils.prepareAvailabilityTime(encounter.getPeriod().getStartElement());
        }

        return StatementTimeMappingUtils.prepareAvailabilityTime(amendedDateTime);
    }

    private Encounter findEncounterForList(ListResource listResource) {
        return (Encounter) messageContext.getInputBundleHolder().getRequiredResource(listResource.getEncounter().getReferenceElement());
    }
}
