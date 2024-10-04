package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static java.util.function.Predicate.not;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.CompoundStatementClassCode.CATEGORY;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.CompoundStatementClassCode.TOPIC;
import static uk.nhs.adaptors.gp2gp.ehr.utils.IgnoredResourcesUtils.isIgnoredResourceType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
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
    private static final String RELATED_PROBLEM_EXTENSION_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect"
        + "-RelatedProblemHeader-1";
    private static final String RELATED_PROBLEM_TARGET = "target";
    private static final String COMPLETE_CODE = "COMPLETE";
    private static final String LIST_REFERENCE_PATTERN = "^List/[\\da-zA-z-]+$";
    private static final String CONTAINED_RESOURCE_REFERENCE_PATTERN = "^List/([\\da-zA-Z-]+)(#[\\da-zA-Z-]+)$";

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
        return messageContext
            .getInputBundleHolder()
            .getListReferencedToEncounter(encounter.getIdElement(), CONSULTATION_LIST_CODE)
            .map(this::mapConsultationListToComponent)
            .orElse(StringUtils.EMPTY);
    }

    private String mapConsultationListToComponent(ListResource listResource) {
        return listResource.getEntry()
            .stream()
            .map(EncounterComponentsMapper::getReferenceElement)
            .map(this::getRequiredResource)
            .map(ListResource.class::cast)
            .map(this::mapTopicListToComponent)
            .collect(Collectors.joining());
    }

    private String mapTopicListToComponent(ListResource topicList) {

        if (!CodeableConceptMappingUtils.hasCode(topicList.getCode(), List.of(TOPIC_LIST_CODE))) {
            throw new EhrMapperException(String.format("Unexpected list %s referenced in Consultation, expected list to be coded as "
                + "Topic (EHR)", topicList.getId()));
        }

        return buildCompoundStatement(
            topicList,
            TOPIC.getCode(),
            prepareCdForTopic(topicList),
            false,
            mapTopicListComponents(topicList)
        );
    }

    private String mapTopicListComponents(ListResource topicList) {
        return String.join(
            StringUtils.EMPTY,
            mapCategorizedResources(topicList),
            mapContainedResources(topicList),
            mapUncategorizedResources(topicList)
        );
    }

    private String mapCategorizedResources(ListResource topicList) {
        return topicList.getEntry().stream()
            .map(EncounterComponentsMapper::getReferenceElement)
            .filter(reference -> reference
                .getValue()
                .matches(LIST_REFERENCE_PATTERN)
            )
            .map(this::getRequiredResource)
            .map(ListResource.class::cast)
            .map(this::mapCategoryListToComponent)
            .collect(Collectors.joining());
    }

    private String mapCategoryListToComponent(ListResource categoryList) {
        if (!CodeableConceptMappingUtils.hasCode(categoryList.getCode(), List.of(CATEGORY_LIST_CODE))) {
            throw new EhrMapperException(
                    "Unexpected list %s referenced in Consultation, expected list to be coded as Category (EHR) or be a container"
                            .formatted(categoryList.getId())
            );
        }

        return buildCompoundStatement(
            categoryList,
            CATEGORY.getCode(),
            prepareCdForCategory(categoryList),
            true,
            mapListResourceToComponents(categoryList)
        );
    }

    private String mapContainedResources(ListResource topicList) {
        return topicList.getEntry().stream()
            .filter(entry -> getReferenceElement(entry)
                .getValue()
                .matches(CONTAINED_RESOURCE_REFERENCE_PATTERN)
            )
            .map(this::mapItemToComponent)
            .flatMap(Optional::stream)
            .collect(Collectors.joining());
    }

    private String mapUncategorizedResources(ListResource topicList) {
        var components = topicList.getEntry().stream()
            .map(EncounterComponentsMapper::getReferenceElement)
            .filter(reference ->
                !reference.getValue().matches(LIST_REFERENCE_PATTERN)
                && !reference.getValue().matches(CONTAINED_RESOURCE_REFERENCE_PATTERN)
            )
            .map(this::getRequiredResource)
            .map(this::mapConsultationListResourceToComponent)
            .flatMap(Optional::stream)
            .collect(Collectors.joining());

        return buildCompoundStatement(
            topicList,
            CATEGORY.getCode(),
            codeableConceptCdMapper.getCdForCategory(),
            true,
            components
        );
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

        var reference = getReferenceElement(item);
        if (isListResource(reference)) {
            return mapResourceContainedInList(reference, this::mapConsultationListResourceToComponent);
        }

        Resource resource = getRequiredResource(reference);

        LOGGER.debug("Translating list entry resource {}", resource.getId());
        return mapConsultationListResourceToComponent(resource);
    }

    private Optional<String> mapConsultationListResourceToComponent(Resource resource) {
        if (encounterComponents.containsKey(resource.getResourceType())) {
            return encounterComponents.get(resource.getResourceType()).apply(resource);
        }

        if (isIgnoredResourceType(resource.getResourceType())) {
            LOGGER.info("Resource of type: {} has been ignored", resource.getResourceType());
            return Optional.empty();
        }

        throw new EhrMapperException("Unsupported resource in consultation list: %s".formatted(resource.getId()));
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
            .filter(not(MedicationRequestUtils::isStoppedMedicationOrder))
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
            diaryPlanStatementMapper.mapProcedureRequestToPlanStatement((ProcedureRequest) resource, IS_NESTED)
        );
    }

    private Optional<String> mapReferralRequest(Resource resource) {
        return Optional.of(requestStatementMapper.mapReferralRequestToRequestStatement((ReferralRequest) resource, IS_NESTED));
    }

    private Optional<String> mapDiagnosticReport(Resource resource) {
        return Optional.of(diagnosticReportMapper.mapDiagnosticReportToCompoundStatement((DiagnosticReport) resource));
    }

    private String prepareCdForTopic(ListResource topicList) {
        final Optional<CodeableConcept> relatedProblem = getConditionReference(topicList)
            .map(reference -> (Condition) getRequiredResource(reference.getReferenceElement()))
            .map(Condition::getCode);

        final var title = topicList.getTitle();

        if (relatedProblem.isPresent() && StringUtils.isNotEmpty(title)) {
            return codeableConceptCdMapper.mapToCdForTopic(relatedProblem.orElseThrow(), title);
        }

        if (relatedProblem.isPresent()) {
            return codeableConceptCdMapper.mapToCdForTopic(relatedProblem.orElseThrow());
        }

        if (StringUtils.isNotEmpty(title)) {
            return codeableConceptCdMapper.mapToCdForTopic(title);
        }

        return codeableConceptCdMapper.getCdForTopic();
    }

    private static Optional<Reference> getConditionReference(ListResource topicList) {
        return topicList.getExtension()
            .stream()
            .filter(ext -> ext.getUrl().equals(RELATED_PROBLEM_EXTENSION_URL))
            .flatMap(ext -> ext.getExtension().stream())
            .filter(relatedProblemExt -> relatedProblemExt.getUrl().equals(RELATED_PROBLEM_TARGET))
            .findFirst()
            .map(ext -> (Reference) ext.getValue());
    }

    private String prepareCdForCategory(ListResource categoryList) {
        Optional<String> title = Optional.ofNullable(categoryList.getTitle());

        return title
            .map(codeableConceptCdMapper::mapToCdForCategory)
            .orElse(codeableConceptCdMapper.getCdForCategory());
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
        return (Encounter) getRequiredResource(listResource.getEncounter().getReferenceElement());
    }

    private Optional<String> mapResourceContainedInList(IIdType fullReference, Function<Resource, Optional<String>> mapperFunction) {
        var pattern = Pattern.compile(CONTAINED_RESOURCE_REFERENCE_PATTERN);
        var matcher = pattern.matcher(fullReference.getValue());

        if (!matcher.find()) {
            return Optional.empty();
        }

        var listId = matcher.group(1);
        var containedResourceId = matcher.group(2);

        var container = (ListResource) getRequiredResource(buildListReference(listId));

        return container.getContained().stream()
            .filter(resource -> resourceHasId(resource, containedResourceId))
            .map(this::removeNumberSignFromId)
            .findFirst()
            .flatMap(mapperFunction);
    }

    private Resource removeNumberSignFromId(Resource resource) {
        return resource.setIdElement(new IdType(resource.getResourceType().name(), resource.getId().replace("#", StringUtils.EMPTY)));
    }

    private boolean resourceHasId(Resource resource, String id) {
        return resource.hasIdElement() && resource.getIdElement().getValue().equals(id);
    }

    private IIdType buildListReference(String id) {
        var referenceString = String.format("%s/%s", ResourceType.List, id);
        return new Reference(referenceString).getReferenceElement();
    }

    private boolean isListResource(IIdType reference) {
        return reference.hasResourceType() && reference.getResourceType().equals(ResourceType.List.name());
    }

    private String buildCompoundStatement(
        ListResource topicList,
        String classCode,
        String compoundStatementCode,
        boolean nested,
        String components
    ) {
        if (StringUtils.isEmpty(components)) {
            return StringUtils.EMPTY;
        }

        String effectiveTime = prepareEffectiveTime(topicList);
        String availabilityTime = prepareAvailabilityTime(topicList);

        var params = CompoundStatementParameters.builder()
            .nested(nested)
            .id(messageContext.getIdMapper().getOrNew(ResourceType.List, topicList.getIdElement()))
            .classCode(classCode)
            .compoundStatementCode(compoundStatementCode)
            .statusCode(COMPLETE_CODE)
            .effectiveTime(effectiveTime)
            .availabilityTime(availabilityTime)
            .components(components)
            .build();

        return TemplateUtils.fillTemplate(COMPOUND_STATEMENT_TEMPLATE, params);
    }

    private static IIdType getReferenceElement(ListResource.ListEntryComponent entry) {
        return entry.getItem().getReferenceElement();
    }

    private Resource getRequiredResource(IIdType reference) {
        return messageContext.getInputBundleHolder().getRequiredResource(reference);
    }
}
