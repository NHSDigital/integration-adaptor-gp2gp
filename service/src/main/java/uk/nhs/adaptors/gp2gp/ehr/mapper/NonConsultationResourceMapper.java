package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.XpathExtractor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class NonConsultationResourceMapper {
    private static final Mustache ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_encounter_to_ehr_composition_template.mustache");
    private static final String COMPLETE_CODE = "COMPLETE";
    private static final String DEFAULT_CODE = "<code code=\"196401000000100\" displayName=\"Non-consultation data\" codeSystem=\"2.16"
        + ".840.1.113883.2.1.3.2.4.15\"/>";
    private static final String OBSERVATION_COMMENT_CODE = "<code code=\"109341000000100\" displayName=\"GP to GP communication "
        + "transaction\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\"/>";
    private static final String BLOOD_PRESSURE_CODE_2 = "<code code=\"109341000000100\" displayName=\"GP to GP communication transaction\""
        + " codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\"/>";
    private static final String DIAGNOSTIC_REPORT_CODE = "<code code=\"109341000000100\" displayName=\"GP to GP communication "
        + "transaction\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\"/>";
    private static final String MEDICATION_REQUEST_CODE = "<code code=\"196391000000103\" displayName=\"Non-consultation medication "
        + "data\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\"/>";
    private static final String CONDITION_CODE = "<code code=\"109341000000100\" displayName=\"GP to GP communication transaction\" "
        + "codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\"/>";
    private static final String QUESTIONNAIRE_RESPONSE_CODE = "<code code=\"109341000000100\" displayName=\"GP to GP communication "
        + "transaction\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\"/>";
    private static final String LOG_TEMPLATE = "Non-consultation resources mapped: %s";

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EncounterComponentsMapper encounterComponentsMapper;
    private final Map<ResourceType, Function<String, EncounterTemplateParameters.EncounterTemplateParametersBuilder>> resourceBuilder =
        Map.of(
            ResourceType.Immunization, this::buildImmunization,
            ResourceType.AllergyIntolerance, this::buildAllergyIntolerance,
            ResourceType.ReferralRequest, this::buildReferralRequest,
//          ResourceType.DiagnosticReport, this::buildDiagnosticRequest,
            ResourceType.MedicationRequest, this::buildMedicationRequest,
            ResourceType.Condition, this::buildCondition,
            ResourceType.ProcedureRequest, this::buildProcedureRequest,
            ResourceType.DocumentReference, this::buildDocumentReference);
//          ResourceType.QuestionnaireResponse, this::buildQuestionnaireResponse);

    public List<String> mapRemainingResourcesToEhrCompositions(Bundle bundle) {
        var mappedResources = bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> !messageContext.getIdMapper().hasIdBeenMapped(resource.getResourceType(), resource.getId()))
            .filter(this::isMappableNonConsultationResource)
            .map(this::mapResourceToEhrComposition)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
        LOGGER.debug(String.format(LOG_TEMPLATE, mappedResources.size()));
        return mappedResources;
    }

    private String mapResourceToEhrComposition(Resource resource) {
        String component = encounterComponentsMapper.mapResourceToComponent(resource);
        EncounterTemplateParameters.EncounterTemplateParametersBuilder builder = null;
        if (resource.getResourceType().equals(ResourceType.Observation)) {
            builder = buildObservation(component, (Observation) resource);
        } else {
            builder = resourceBuilder.getOrDefault(resource.getResourceType(), this::notMapped)
                .apply(component);
        }

        if (builder != null) {
            builder.encounterStatementId(randomIdGeneratorService.createNewId())
                .status(COMPLETE_CODE)
                .components(component);
            return TemplateUtils.fillTemplate(ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
                builder.build());
        }
        return StringUtils.EMPTY;
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildUncategorisedObservation(String component) {
        return XpathExtractor.extractValuesForUncategorizedObservation(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildCommentObservation(String component) {
        return XpathExtractor.extractValuesForCommentObservation(component)
            .altCode(OBSERVATION_COMMENT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildImmunization(String component) {
        return XpathExtractor.extractValuesForImmunization(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildAllergyIntolerance(String component) {
        return XpathExtractor.extractValuesForAllergyIntolerance(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildBloodPressureObservation(String component) {
        return XpathExtractor.extractValuesForBloodPressure(component)
            .altCode(BLOOD_PRESSURE_CODE_2);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildReferralRequest(String component) {
        return XpathExtractor.extractValuesForReferralRequest(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildDiagnosticRequest(String component) {
        return null;
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildMedicationRequest(String component) {
        return XpathExtractor.extractValuesForMedicationRequest(component)
            .altCode(MEDICATION_REQUEST_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildCondition(String component) {
        return XpathExtractor.extractValuesForCondition(component)
            .altCode(CONDITION_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildProcedureRequest(String component) {
        return XpathExtractor.extractValuesForProcedureRequest(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildDocumentReference(String component) {
        return XpathExtractor.extractValuesForDocumentReference(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildQuestionnaireResponse(String component) {
        return null;
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder buildObservation(String component, Observation observation) {
        return mapObservation(observation, component);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder mapObservation(Resource resource, String component) {
        Observation observation = (Observation) resource;
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(EncounterComponentsMapper.NARRATIVE_STATEMENT_CODE))) {
            return buildCommentObservation(component);
        }
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), EncounterComponentsMapper.BLOOD_CODES)) {
            return buildBloodPressureObservation(component);
        }

        return buildUncategorisedObservation(component);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder notMapped(String component) {
        return null;
    }

    private boolean isMappableNonConsultationResource(Resource resource) {
        if (resource.getResourceType().equals(ResourceType.Observation)) {
            return true;
        }
        return resourceBuilder.containsKey(resource.getResourceType());
    }
}
