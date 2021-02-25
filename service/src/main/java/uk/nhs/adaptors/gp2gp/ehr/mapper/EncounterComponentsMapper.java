package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EncounterComponentsMapper {

    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final String TOPIC_LIST_CODE = "25851000000105";
    private static final String CATEGORY_LIST_CODE = "24781000000107";
    private static final List<String> COMPONENTS_LISTS = Arrays.asList(TOPIC_LIST_CODE, CATEGORY_LIST_CODE);

    private static final String NARRATIVE_STATEMENT_CODE = "37331000000100";
    private static final String NOT_IMPLEMENTED_MAPPER_PLACE_HOLDER = "<!-- %s/%s -->";
    private static final boolean IS_NESTED = false;

    private final Map<ResourceType, Function<Resource, String>> encounterComponents = ImmutableMap.<ResourceType, Function<Resource,
        String>>builder()
        .put(ResourceType.List, this::mapListResource)
        .put(ResourceType.ProcedureRequest, this::mapProcedureRequest)
        .put(ResourceType.Observation, this::mapObservation)
        .put(ResourceType.Immunization, this::mapImmunization)
        .put(ResourceType.Condition, this::mapCondition)
        .build();

    private final MessageContext messageContext;

    private final DiaryPlanStatementMapper diaryPlanStatementMapper;

    private final NarrativeStatementMapper narrativeStatementMapper;
    private final ObservationStatementMapper observationStatementMapper;
    private final ImmunizationObservationStatementMapper immunizationObservationStatementMapper;
    private final ConditionLinkSetMapper conditionLinkSetMapper;

    public String mapComponents(Encounter encounter) {
        Optional<ListResource> listReferencedToEncounter =
            messageContext.getInputBundleHolder().getListReferencedToEncounter(encounter.getIdElement(), CONSULTATION_LIST_CODE);

        return listReferencedToEncounter
            .map(this::mapListResourceToComponents)
            .orElse(StringUtils.EMPTY);
    }

    private String mapListResourceToComponents(ListResource listReferencedToEncounter) {
        return listReferencedToEncounter.getEntry()
            .stream()
            .map(this::mapItemToComponent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());
    }

    private Optional<String> mapItemToComponent(ListResource.ListEntryComponent item) {
        Optional<Resource> resource = messageContext.getInputBundleHolder().getResource(item.getItem().getReferenceElement());

        return resource.map(value -> encounterComponents.getOrDefault(value.getResourceType(), this::mapDefaultNotImplemented)
            .apply(value));
    }

    private String mapDefaultNotImplemented(Resource resource) {
        return String.format(NOT_IMPLEMENTED_MAPPER_PLACE_HOLDER,
            resource.getIdElement().getResourceType(),
            resource.getIdElement().getIdPart());
    }

    private String mapCondition(Resource resource) {
        return conditionLinkSetMapper.mapConditionToLinkSet((Condition) resource, IS_NESTED);
    }

    private String mapProcedureRequest(Resource resource) {
        return diaryPlanStatementMapper.mapDiaryProcedureRequestToPlanStatement((ProcedureRequest) resource, IS_NESTED);
    }

    private String mapObservation(Resource resource) {
        Observation observation = (Observation) resource;
        if (hasCode(observation.getCode(), List.of(NARRATIVE_STATEMENT_CODE))) {
            return narrativeStatementMapper.mapObservationToNarrativeStatement(observation, IS_NESTED);
        }

        return observationStatementMapper.mapObservationToObservationStatement(observation, IS_NESTED);
    }

    private String mapImmunization(Resource resource) {
        return immunizationObservationStatementMapper.mapImmunizationToObservationStatement((Immunization) resource, IS_NESTED);
    }

    private String mapListResource(Resource resource) {
        ListResource listResource = (ListResource) resource;

        if (listResource.hasEntry() && hasCode(listResource.getCode(), COMPONENTS_LISTS)) {
            return mapListResourceToComponents(listResource);
        }

        return StringUtils.EMPTY;
    }

    private boolean hasCode(CodeableConcept code, List<String> codeLists) {
        return code != null && code.getCoding()
            .stream()
            .anyMatch(coding -> codeLists.contains(coding.getCode()));
    }
}
