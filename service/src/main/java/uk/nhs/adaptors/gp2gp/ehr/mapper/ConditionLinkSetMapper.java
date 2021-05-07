package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.hl7.fhir.dstu3.model.Resource;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ConditionLinkSetMapperParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ConditionLinkSetMapper {

    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_link_set_template.mustache");
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
    private static final String LIST = "List";

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final ParticipantMapper participantMapper;

    public String mapConditionToLinkSet(Condition condition, boolean isNested) {
        if (!condition.hasAsserter()) {
            throw new EhrMapperException("Condition.asserter is required");
        }

        final IdMapper idMapper = messageContext.getIdMapper();
        var builder = ConditionLinkSetMapperParameters.builder()
            .isNested(isNested)
            .linkSetId(idMapper.getOrNew(ResourceType.Condition, condition.getIdElement()));

        buildEffectiveTimeLow(condition).ifPresent(builder::effectiveTimeLow);
        buildEffectiveTimeHigh(condition).ifPresent(builder::effectiveTimeHigh);
        buildAvailabilityTime(condition).ifPresent(builder::availabilityTime);
        builder.relatedClinicalContent(buildRelatedClinicalContent(condition));

        buildQualifier(condition).ifPresent(qualifier -> setQualifierProperties(builder, qualifier));
        buildClinicalStatusCode(condition).ifPresent(status -> setClinicalStatusProperties(builder, status));

        buildConditionNamed(condition).ifPresentOrElse(builder::conditionNamed,
            () -> {
                String newId = randomIdGeneratorService.createNewId();
                builder.generateObservationStatement(true);
                builder.conditionNamed(newId);
                buildPertinentInfo(condition).ifPresent(builder::pertinentInfo);
            });
        var asserterReference = condition.getAsserter();
        var performerReference = idMapper.get(asserterReference);
        var referenceElement = asserterReference.getReferenceElement();

        messageContext.getInputBundleHolder().getResource(referenceElement)
            .map(Resource::getResourceType)
            .filter(ResourceType.Practitioner::equals)
            .orElseThrow(() -> new EhrMapperException("Condition.asserter must be a Practitioner"));
        var performerParameter = participantMapper.mapToParticipant(performerReference, ParticipantType.PERFORMER);
        builder.performer(performerParameter);

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
            .map(value -> (Reference) value)
            .filter(this::checkIfReferenceIsObservation)
            .map(reference -> messageContext.getIdMapper().getOrNew(reference));
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
           .map(value -> (Reference) value)
           .filter(this::filterOutNonExistentResource)
           .filter(this::filterOutListResourceType)
           .map(reference -> messageContext.getIdMapper().getOrNew(reference))
           .collect(Collectors.toList());
    }

    private Optional<String> buildPertinentInfo(Condition condition) {
        if (condition.hasNote()) {
            return Optional.of(condition.getNote()
                .stream()
                .map(Annotation::getText)
                .collect(Collectors.joining(StringUtils.SPACE)));
        }
        return Optional.empty();
    }

    private boolean filterOutNonExistentResource(Reference reference) {

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

    private boolean filterOutListResourceType(Reference reference) {
        return !reference.getReferenceElement().getResourceType().equals(LIST);
    }

    private boolean checkIfReferenceIsObservation(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals(ResourceType.Observation.name());
    }
}
