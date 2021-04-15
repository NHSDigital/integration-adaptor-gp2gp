package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.XpathExtractor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
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

    private static final String BLOOD_PRESSURE_READING_CODE = "163020007";
    private static final String ARTERIAL_BLOOD_PRESSURE_CODE = "386534000";
    private static final String BLOOD_PRESSURE_CODE = "75367002";
    private static final String STANDING_BLOOD_PRESSURE_CODE = "163034007";
    private static final String SITTING_BLOOD_PRESSURE_CODE = "163035008";
    private static final String LAYING_BLOOD_PRESSURE_CODE = "163033001";
    private static final List<String> BLOOD_CODES = Arrays.asList(BLOOD_PRESSURE_READING_CODE, ARTERIAL_BLOOD_PRESSURE_CODE,
        BLOOD_PRESSURE_CODE, STANDING_BLOOD_PRESSURE_CODE, SITTING_BLOOD_PRESSURE_CODE, LAYING_BLOOD_PRESSURE_CODE);

    private static final String NARRATIVE_STATEMENT_CODE = "37331000000100";

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EncounterComponentsMapper encounterComponentsMapper;
    private final Map<ResourceType, Function<String, EncounterTemplateParameters.EncounterTemplateParametersBuilder>> resourceBuilder =
        ImmutableMap.<ResourceType, Function<String, EncounterTemplateParameters.EncounterTemplateParametersBuilder>>builder()
            .put(ResourceType.Immunization, this::buildImmunization)
            .put(ResourceType.AllergyIntolerance, this::buildAllergyIntolerance)
            .put(ResourceType.ReferralRequest, this::buildReferralRequest)
//            .put(ResourceType.DiagnosticReport, this::buildDiagnosticRequest)
            .put(ResourceType.MedicationRequest, this::buildMedicationRequest)
            .put(ResourceType.Condition, this::buildCondition)
            .put(ResourceType.ProcedureRequest, this::buildProcedureRequest)
            .put(ResourceType.DocumentReference, this::buildDocumentReference)
//            .put(ResourceType.QuestionnaireResponse, this::buildQuestionnaireResponse)
            .build();

    public List<String> mapRemainingResourcesToEhrCompositions(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> !messageContext.getIdMapper().hasIdBeenMapped(resource.getResourceType(), resource.getId()))
            .map(this::mapResourceToEhrComposition)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }

    private String mapResourceToEhrComposition(Resource resource) {
        String component = encounterComponentsMapper.mapResourceToComponent(resource);
        System.out.println(component);
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
        if (hasCode(observation.getCode(), List.of(NARRATIVE_STATEMENT_CODE))) {
            return buildCommentObservation(component);
        }
        if (hasCode(observation.getCode(), BLOOD_CODES)) {
            return buildBloodPressureObservation(component);
        }

        return buildUncategorisedObservation(component);
    }

    private EncounterTemplateParameters.EncounterTemplateParametersBuilder notMapped(String component) {
        return null;
    }

    private boolean hasCode(CodeableConcept code, List<String> codeLists) {
        return code != null && code.getCoding()
            .stream()
            .anyMatch(coding -> codeLists.contains(coding.getCode()));
    }
}
