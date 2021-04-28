package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareEffectiveTimeForNonConsultation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters.EncounterTemplateParametersBuilder;

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
    private static final String NULL_FLAVOR = "nullFlavor";

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EncounterComponentsMapper encounterComponentsMapper;
    private final Map<ResourceType, Function<String, EncounterTemplateParameters.EncounterTemplateParametersBuilder>> resourceBuilder =
        Map.of(
            ResourceType.Immunization, this::buildForImmunization,
            ResourceType.AllergyIntolerance, this::buildForAllergyIntolerance,
            ResourceType.ReferralRequest, this::buildForReferralRequest,
            ResourceType.MedicationRequest, this::buildForMedicationRequest,
            ResourceType.Condition, this::buildForCondition,
            ResourceType.ProcedureRequest, this::buildForProcedureRequest,
            ResourceType.DocumentReference, this::buildForDocumentReference,
            ResourceType.QuestionnaireResponse, this::buildForQuestionnaireResponse);

    public List<String> mapRemainingResourcesToEhrCompositions(Bundle bundle) {
        var mappedResources = bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> !hasIdBeenMapped(resource))
            .filter(this::isMappableNonConsultationResource)
            .map(this::mapResourceToEhrComposition)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        LOGGER.debug("Non-consultation resources mapped: {}", mappedResources.size());
        return mappedResources;
    }

    private Optional<String> mapResourceToEhrComposition(Resource resource) {
        String component = encounterComponentsMapper.mapResourceToComponent(resource);
        EncounterTemplateParametersBuilder builder;
        if (resource.getResourceType().equals(ResourceType.Observation)) {
            builder = buildForObservation(component, (Observation) resource);
        } else if (resource.getResourceType().equals(ResourceType.DiagnosticReport)) {
            builder = buildForDiagnosticReport(component, (DiagnosticReport) resource);
        } else {
            builder = resourceBuilder.getOrDefault(resource.getResourceType(), this::notMapped)
                .apply(component);
        }

        if (builder != null) {
            builder.encounterStatementId(randomIdGeneratorService.createNewId())
                .status(COMPLETE_CODE)
                .components(component);
            EncounterTemplateParameters build = builder.build();

            String effectiveTime = build.getEffectiveTime();
            messageContext.getEffectiveTime().updateEffectiveTimeLowFormatted(effectiveTime);
            build.setEffectiveTime(prepareEffectiveTimeForNonConsultation(effectiveTime));

            return Optional.of(
                TemplateUtils.fillTemplate(
                    ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
                    build)
            );
        }
        return Optional.empty();
    }

    private EncounterTemplateParametersBuilder buildForUncategorisedObservation(String component) {
        return XpathExtractor.extractValuesForUncategorizedObservation(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForCommentObservation(String component) {
        return XpathExtractor.extractValuesForCommentObservation(component)
            .altCode(OBSERVATION_COMMENT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForImmunization(String component) {
        return XpathExtractor.extractValuesForImmunization(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForAllergyIntolerance(String component) {
        return XpathExtractor.extractValuesForAllergyIntolerance(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForBloodPressureObservation(String component) {
        return XpathExtractor.extractValuesForBloodPressure(component)
            .altCode(BLOOD_PRESSURE_CODE_2);
    }

    private EncounterTemplateParametersBuilder buildForReferralRequest(String component) {
        return XpathExtractor.extractValuesForReferralRequest(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForDiagnosticReport(String component, DiagnosticReport diagnosticReport) {
        boolean isAgentPerson = false;

        if (diagnosticReport.hasPerformer()) {
            isAgentPerson = Optional.of(diagnosticReport.getPerformerFirstRep())
                .map(DiagnosticReport.DiagnosticReportPerformerComponent::getActor)
                .map(Reference::getReferenceElement)
                .filter(IIdType::hasResourceType)
                .filter(idType -> idType.getResourceType().equals(ResourceType.Practitioner.name()))
                .isPresent();
        }

        var diagnosticReportXml = XpathExtractor.extractValuesForDiagnosticReport(component)
            .altCode(DIAGNOSTIC_REPORT_CODE);

        if (!isAgentPerson) {
            diagnosticReportXml
                .author(NULL_FLAVOR)
                .participant2(NULL_FLAVOR);
        }

        return diagnosticReportXml;
    }

    private EncounterTemplateParametersBuilder buildForMedicationRequest(String component) {
        return XpathExtractor.extractValuesForMedicationRequest(component)
            .altCode(MEDICATION_REQUEST_CODE);
    }

    private EncounterTemplateParametersBuilder buildForCondition(String component) {
        return XpathExtractor.extractValuesForCondition(component)
            .altCode(CONDITION_CODE);
    }

    private EncounterTemplateParametersBuilder buildForProcedureRequest(String component) {
        return XpathExtractor.extractValuesForProcedureRequest(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForDocumentReference(String component) {
        return XpathExtractor.extractValuesForDocumentReference(component)
            .altCode(DEFAULT_CODE);
    }

    // TODO: Add builder once NIAD-1307 has been completed
    private EncounterTemplateParametersBuilder buildForQuestionnaireResponse(String component) {
        return null;
    }

    private EncounterTemplateParametersBuilder buildForObservation(String component, Observation observation) {
        return mapObservation(observation, component);
    }

    private EncounterTemplateParametersBuilder mapObservation(Resource resource, String component) {
        Observation observation = (Observation) resource;
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(EncounterComponentsMapper.NARRATIVE_STATEMENT_CODE))) {
            return buildForCommentObservation(component);
        }
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), EncounterComponentsMapper.BLOOD_CODES)) {
            return buildForBloodPressureObservation(component);
        }

        return buildForUncategorisedObservation(component);
    }

    private EncounterTemplateParametersBuilder notMapped(String component) {
        return null;
    }

    private boolean isMappableNonConsultationResource(Resource resource) {
        return resource.getResourceType().equals(ResourceType.Observation)
            || resource.getResourceType().equals(ResourceType.DiagnosticReport)
            || resourceBuilder.containsKey(resource.getResourceType());
    }

    private boolean hasIdBeenMapped(Resource resource) {
        return messageContext.getIdMapper().hasIdBeenMapped(resource.getResourceType(), resource.getId());
    }
}
