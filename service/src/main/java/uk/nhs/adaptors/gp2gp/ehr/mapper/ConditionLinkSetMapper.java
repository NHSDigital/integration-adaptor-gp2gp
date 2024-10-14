package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.MedicationRequestUtils.isMedicationRequestType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ConditionLinkSetMapperParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper.ConditionWrapper;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.MedicationRequestUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ConditionLinkSetMapper {

    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_link_set_template.mustache");
    private static final String ACTUAL_PROBLEM_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ActualProblem-1";
    private static final String PROBLEM_SIGNIFICANCE_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ProblemSignificance-1";
    private static final String RELATED_CLINICAL_CONTENT_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-RelatedClinicalContent-1";
    private static final String ACTIVE = "Active";
    private static final String ACTIVE_CODE = "394774009";
    private static final String INACTIVE = "Inactive";
    private static final String INACTIVE_CODE = "394775005";
    private static final String PROBLEM = " Problem";
    private static final String MAJOR = "Major";
    private static final String MAJOR_CODE = "386134007";
    private static final String SIGNIFICANT = "Significant";
    private static final String MINOR = "Minor";
    private static final String MINOR_CODE = "394847000";
    private static final String UNSPECIFIED_SIGNIFICANCE = "Unspecified significance";
    private static final List<String> SUPPRESSED_LINKAGE_RESOURCES = List.of("List", "Encounter");

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;
    private final ConfidentialityService confidentialityService;

    public String mapConditionToLinkSet(Condition condition, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();
        var builder = ConditionLinkSetMapperParameters.builder()
            .isNested(isNested)
            .linkSetId(idMapper.getOrNew(ResourceType.Condition, condition.getIdElement()));

        buildEffectiveTimeLow(condition).ifPresent(builder::effectiveTimeLow);
        buildEffectiveTimeHigh(condition).ifPresent(builder::effectiveTimeHigh);
        buildAvailabilityTime(condition).ifPresent(builder::availabilityTime);
        builder.confidentialityCode(confidentialityService.generateConfidentialityCode(condition).orElse(null));
        builder.relatedClinicalContent(buildRelatedClinicalContent(condition));

        buildQualifier(condition).ifPresent(qualifier -> setQualifierProperties(builder, qualifier));
        buildClinicalStatusCode(condition).ifPresent(status -> setClinicalStatusProperties(builder, status));

        buildConditionNamed(condition).ifPresentOrElse(builder::conditionNamed,
            () -> {
                String newId = randomIdGeneratorService.createNewId();
                builder.generateObservationStatement(true);
                builder.conditionNamed(newId);
                buildObservationStatementAvailabilityTime(condition).ifPresent(builder::observationStatementAvailabilityTime);
                new ConditionWrapper(condition, messageContext, codeableConceptCdMapper)
                    .buildProblemInfo().ifPresent(builder::pertinentInfo);
                builder.actualProblemLinkId(buildActualProblemLinkId(condition));
            });

        builder.code(buildCode(condition));

        if (condition.hasAsserter()) {
            var asserterReference = condition.getAsserter();
            var performerReference = messageContext.getAgentDirectory().getAgentId(asserterReference);

            var referenceElement = asserterReference.getReferenceElement();
            messageContext.getInputBundleHolder().getResource(referenceElement)
                    .map(Resource::getResourceType)
                    .filter(ResourceType.Practitioner::equals)
                    .orElseThrow(() -> new EhrMapperException("Condition.asserter must be a Practitioner"));

            var performerParameter = participantMapper.mapToParticipant(performerReference, ParticipantType.PERFORMER);
            builder.performer(performerParameter);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, builder.build());
    }

    private void setQualifierProperties(ConditionLinkSetMapperParameters.ConditionLinkSetMapperParametersBuilder builder,
        String qualifier) {
        builder.qualifierDisplay(qualifier);
        if (qualifier.equalsIgnoreCase(MAJOR)) {
            builder.qualifierCode(MAJOR_CODE);
            builder.qualifierSignificance(SIGNIFICANT);
        } else if (qualifier.equalsIgnoreCase(MINOR)) {
            builder.qualifierCode(MINOR_CODE);
            builder.qualifierSignificance(UNSPECIFIED_SIGNIFICANCE);
        }
    }

    private void setClinicalStatusProperties(ConditionLinkSetMapperParameters.ConditionLinkSetMapperParametersBuilder builder,
        String clinicalStatus) {
        if (clinicalStatus.equalsIgnoreCase(ACTIVE)) {
            builder.clinicalStatusCode(ACTIVE_CODE);
            builder.clinicalStatusDisplay(ACTIVE + PROBLEM);
        } else if (clinicalStatus.equalsIgnoreCase(INACTIVE)) {
            builder.clinicalStatusCode(INACTIVE_CODE);
            builder.clinicalStatusDisplay(INACTIVE + PROBLEM);
        }
    }

    private Optional<String> buildConditionNamed(Condition condition) {
        return ExtensionMappingUtils.filterExtensionByUrl(condition, ACTUAL_PROBLEM_URL)
                                    .map(Extension::getValue)
                                    .map(Reference.class::cast)
                                    .filter(this::checkIfReferenceIsObservation)
                                    .map(this::mapLinkedId);

    }

    private boolean isTransformedActualProblemHeader(Condition condition) {
        return ExtensionMappingUtils.filterExtensionByUrl(condition, ACTUAL_PROBLEM_URL)
                                    .map(Extension::getValue)
                                    .map(Reference.class::cast)
                                    .filter(reference -> checkIfReferenceIsAllergyIntolerance(reference)
                                                         || checkIfReferenceIsImmunization(reference)
                                                         || checkIfReferenceIsMedicationRequest(reference)).isPresent();
    }

    private Optional<String> buildQualifier(Condition condition) {
        Optional<Extension> problemSignificance = ExtensionMappingUtils.filterExtensionByUrl(condition, PROBLEM_SIGNIFICANCE_URL);
        return problemSignificance.map(extension -> extension.getValue().toString());
    }

    private Optional<String> buildClinicalStatusCode(Condition condition) {
        if (condition.hasClinicalStatus()) {
            return Optional.of(condition.getClinicalStatus().getDisplay());
        }
        return Optional.empty();
    }

    private Optional<String> buildEffectiveTimeLow(Condition condition) {
        if (condition.hasOnset()
            && (condition.hasOnsetDateTimeType() && condition.getOnsetDateTimeType().hasValue())) {
            return Optional.of(DateFormatUtil
                .toHl7Format(condition.getOnsetDateTimeType()));
        }
        return Optional.empty();
    }

    private Optional<String> buildEffectiveTimeHigh(Condition condition) {
        if (condition.hasAbatement()
            && (condition.hasAbatementDateTimeType() && condition.getAbatementDateTimeType().hasValue())) {
            return Optional.of(DateFormatUtil
                .toHl7Format(condition.getAbatementDateTimeType()));
        }
        return Optional.empty();
    }

    private Optional<String> buildAvailabilityTime(Condition condition) {
        if (condition.hasAssertedDate()) {
            return Optional.of(DateFormatUtil
                .toHl7Format(condition.getAssertedDateElement()));
        }
        return Optional.empty();
    }

    private List<String> buildRelatedClinicalContent(Condition condition) {
        return ExtensionMappingUtils.filterAllExtensionsByUrl(condition, RELATED_CLINICAL_CONTENT_URL)
                                    .stream()
                                    .map(Extension::getValue)
                                    .map(Reference.class::cast)
                                    .filter(this::nonExistentResourceFilter)
                                    .filter(this::suppressedLinkageResourcesFilter)
                                    .map(this::mapLinkedId)
                                    .collect(Collectors.toList());
    }

    private boolean nonExistentResourceFilter(Reference reference) {

        var referencePresent = messageContext.getInputBundleHolder()
            .getResource(reference.getReferenceElement())
            .isPresent();
        if (referencePresent) {
            return true;
        }

        // TODO: workaround for NIAD-1409 should throw an exception but public demonstrator includes invalid references
        LOGGER.warn("Condition related clinical context extension uses invalid reference: {}", reference.getReference());
        return false;
    }

    private boolean suppressedLinkageResourcesFilter(Reference reference) {
        return !(isSuppressedResource(reference) || isSuppressedMedicationRequest(reference));
    }

    private boolean isSuppressedResource(Reference reference) {
        return SUPPRESSED_LINKAGE_RESOURCES.contains(reference.getReferenceElement().getResourceType());
    }

    private boolean checkIfReferenceIsObservation(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals(ResourceType.Observation.name());
    }

    private boolean checkIfReferenceIsAllergyIntolerance(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals(ResourceType.AllergyIntolerance.name());
    }

    private boolean checkIfReferenceIsImmunization(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals(ResourceType.Immunization.name());
    }

    private boolean checkIfReferenceIsMedicationRequest(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals(ResourceType.MedicationRequest.name());
    }

    private String buildCode(Condition condition) {
        if (condition.hasCode()) {
            if (isTransformedActualProblemHeader(condition)) {
                return codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(condition.getCode());
            }

            return codeableConceptCdMapper.mapCodeableConceptToCd(condition.getCode());
        }
        throw new EhrMapperException("Condition code not present");
    }

    private boolean isSuppressedMedicationRequest(Reference reference) {
        if (isMedicationRequestType(reference)) {
            return messageContext.getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(MedicationRequest.class::cast)
                .map(MedicationRequestUtils::isStoppedMedicationOrder)
                .orElse(false);
        }

        // for all other types do not suppress with this function
        return false;
    }

    private Optional<String> buildObservationStatementAvailabilityTime(Condition condition) {

        return buildAvailabilityTime(condition);
    }

    private String mapLinkedId(Reference reference) {
        if (checkIfReferenceIsAllergyIntolerance(reference) && reference.getResource() != null) {
            var idType = IdType.of(reference.getResource());
            return messageContext.getIdMapper().getOrNew(ResourceType.Observation, idType);
        }

        return messageContext.getIdMapper().getOrNew(reference);
    }

    private String buildActualProblemLinkId(Condition condition) {
        return ExtensionMappingUtils.filterAllExtensionsByUrl(condition, ACTUAL_PROBLEM_URL)
            .stream()
            .map(Extension::getValue)
            .map(Reference.class::cast)
            .filter(this::nonExistentResourceFilter)
            .filter(this::suppressedLinkageResourcesFilter)
            .map(this::mapLinkedId)
            .findFirst()
            .orElse(StringUtils.EMPTY);
    }
}
