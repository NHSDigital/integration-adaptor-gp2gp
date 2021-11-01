package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareEffectiveTimeForNonConsultation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ListResource;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters.EncounterTemplateParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.IgnoredResourcesUtils;
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
    private static final String ENDED_ALLERGIES_CODE = "1103671000000101";

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EncounterComponentsMapper encounterComponentsMapper;
    private final DocumentReferenceToNarrativeStatementMapper narrativeStatementMapper;
    private final BloodPressureValidator bloodPressureValidator;
    private final Map<ResourceType, BiFunction<String, Resource, EncounterTemplateParametersBuilder>> resourceBuilder =
        Map.of(
            ResourceType.Observation, this::buildForObservation,
            ResourceType.DiagnosticReport, this::buildForDiagnosticReport,
            ResourceType.Immunization, this::buildForImmunization,
            ResourceType.AllergyIntolerance, this::buildForAllergyIntolerance,
            ResourceType.ReferralRequest, this::buildForReferralRequest,
            ResourceType.MedicationRequest, this::buildForMedicationRequest,
            ResourceType.Condition, this::buildForCondition,
            ResourceType.ProcedureRequest, this::buildForProcedureRequest,
            ResourceType.DocumentReference, this::buildForDocumentReference,
            ResourceType.QuestionnaireResponse, this::buildForQuestionnaireResponse
        );

    public List<String> mapRemainingResourcesToEhrCompositions(Bundle bundle) {
        var mappedResources = bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(this::isMappableNonConsultationResource)
            .sorted(this::compareProcessingOrder)
            .filter(resource -> !hasIdBeenMapped(resource) && !isIgnoredResource(resource))
            .map(this::mapResourceToEhrComposition)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        var endedAllergies = bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(this::isEndedAllergyList)
            .map(ListResource.class::cast)
            .map(ListResource::getContained)
            .flatMap(List::stream)
            .filter(resource -> !hasIdBeenMapped(resource))
            .map(this::replaceId)
            .map(this::mapResourceToEhrComposition)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        mappedResources.addAll(endedAllergies);

        LOGGER.debug("Non-consultation resources mapped: {}", mappedResources.size());
        return mappedResources;
    }

    private Resource replaceId(Resource resource) {
        resource.setIdElement(new IdType(ResourceType.AllergyIntolerance.name(), randomIdGeneratorService.createNewId()));
        return resource;
    }

    public List<String> buildEhrCompositionForSkeletonEhrExtract(String bindingDocumentId) {
        var narrativeStatement = narrativeStatementMapper.buildFragmentIndexNarrativeStatement(bindingDocumentId);
        EncounterTemplateParameters encounterTemplateParameters = EncounterTemplateParameters.builder()
            .components(narrativeStatement)
            .build();
        return List.of(TemplateUtils.fillTemplate(
            ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
            encounterTemplateParameters)
        );
    }

    private int compareProcessingOrder(Resource resource1, Resource resource2) {
        return Integer.compare(
            getProcessingOrder(resource1.getResourceType()),
            getProcessingOrder(resource2.getResourceType())
        );
    }

    private int getProcessingOrder(ResourceType resourceType) {
        // Observations need to be processed after DiagnosticReports, to prevent their possible duplication (NIAD-1464)
        if (resourceType == ResourceType.Observation) {
            return 1;
        } else {
            return 0;
        }
    }

    private Optional<String> mapResourceToEhrComposition(Resource resource) {

        Optional<String> componentHolder = encounterComponentsMapper.mapResourceToComponent(resource);

        if (componentHolder.isEmpty()) {
            LOGGER.warn("Skipping {} with ID '{}'. The mapping output contains blank XML statement content",
                resource.getResourceType(), resource.getId());
            return Optional.empty();
        }

        String component = componentHolder.get();
        EncounterTemplateParametersBuilder builder = resourceBuilder
            .getOrDefault(resource.getResourceType(), this::notMapped)
            .apply(component, resource);

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

    private EncounterTemplateParametersBuilder buildForImmunization(String component, Resource resource) {
        return XpathExtractor.extractValuesForImmunization(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForAllergyIntolerance(String component, Resource resource) {
        return XpathExtractor.extractValuesForAllergyIntolerance(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForBloodPressureObservation(String component) {
        return XpathExtractor.extractValuesForBloodPressure(component)
            .altCode(BLOOD_PRESSURE_CODE_2);
    }

    private EncounterTemplateParametersBuilder buildForReferralRequest(String component, Resource resource) {
        return XpathExtractor.extractValuesForReferralRequest(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForDiagnosticReport(String component, Resource resource) {
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;

        var diagnosticReportXml = XpathExtractor.extractValuesForDiagnosticReport(component)
            .altCode(DIAGNOSTIC_REPORT_CODE);

        boolean isAgentPerson = diagnosticReport.hasPerformer()
            && Optional.of(diagnosticReport.getPerformerFirstRep())
            .map(DiagnosticReport.DiagnosticReportPerformerComponent::getActor)
            .map(Reference::getReferenceElement)
            .filter(IIdType::hasResourceType)
            .map(IIdType::getResourceType)
            .filter(resourceType -> ResourceType.Practitioner.name().equals(resourceType)
                || ResourceType.Organization.name().equals(resourceType))
            .isPresent();
        if (!isAgentPerson) {
            diagnosticReportXml
                .author(null)
                .participant2(null);
        }

        return diagnosticReportXml;
    }

    private EncounterTemplateParametersBuilder buildForMedicationRequest(String component, Resource resource) {
        return XpathExtractor.extractValuesForMedicationRequest(component)
            .altCode(MEDICATION_REQUEST_CODE);
    }

    private EncounterTemplateParametersBuilder buildForCondition(String component, Resource resource) {
        return XpathExtractor.extractValuesForCondition(component)
            .altCode(CONDITION_CODE);
    }

    private EncounterTemplateParametersBuilder buildForProcedureRequest(String component, Resource resource) {
        return XpathExtractor.extractValuesForProcedureRequest(component)
            .altCode(DEFAULT_CODE);
    }

    private EncounterTemplateParametersBuilder buildForDocumentReference(String component, Resource resource) {
        return XpathExtractor.extractValuesForDocumentReference(component)
            .altCode(DEFAULT_CODE);
    }

    // TODO: Add builder once NIAD-1307 has been completed
    private EncounterTemplateParametersBuilder buildForQuestionnaireResponse(String component, Resource resource) {
        return null;
    }

    private EncounterTemplateParametersBuilder buildForObservation(String component, Resource resource) {
        Observation observation = (Observation) resource;
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(EncounterComponentsMapper.NARRATIVE_STATEMENT_CODE))) {
            return buildForCommentObservation(component);
        }
        if (bloodPressureValidator.isValidBloodPressure(observation)) {
            return buildForBloodPressureObservation(component);
        }

        return buildForUncategorisedObservation(component);
    }

    private EncounterTemplateParametersBuilder notMapped(String component, Resource resource) {
        return null;
    }

    private boolean isMappableNonConsultationResource(Resource resource) {
        return resourceBuilder.containsKey(resource.getResourceType());
    }

    private boolean hasIdBeenMapped(Resource resource) {
        return messageContext.getIdMapper().hasIdBeenMapped(resource.getResourceType(), resource.getIdElement());
    }

    private boolean isIgnoredResource(Resource resource) {
        if (IgnoredResourcesUtils.isIgnoredResourceType(resource.getResourceType())) {
            LOGGER.info(String.format("Resource of type: %s has been ignored", resource.getResourceType()));
            return true;
        }

        return false;
    }

    private boolean isEndedAllergyList(Resource resource) {
        if (resource.getResourceType().equals(ResourceType.List)) {
            var list = (ListResource) resource;
            var endedAllergies = Optional.ofNullable(list.getCode())
                .map(CodeableConcept::getCodingFirstRep)
                .filter(coding -> ENDED_ALLERGIES_CODE.equals(coding.getCode()))
                .orElse(null);
            if (endedAllergies != null) {
                return list.hasContained();
            }
        }
        return false;
    }
}
