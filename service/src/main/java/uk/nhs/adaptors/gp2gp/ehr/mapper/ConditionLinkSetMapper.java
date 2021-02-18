package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
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
public class ConditionLinkSetMapper {

    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_link_set_template.mustache");
    private static final String ACTUAL_PROBLEM_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ActualProblem-1";
    private static final String PROBLEM_SIGNIFICANCE_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ProblemSignificance-1";
    private static final String RELATED_CLINICAL_CONTENT_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-RelatedClinicalContent-1";
    private static final String ACTIVE = "active";
    private static final String MAJOR = "major";
    private static final String LIST = "List";
    private static final String EXTENSION_NOT_PRESENT = "Extension not present";

    private final MessageContext messageContext;

    public String mapConditionToLinkSet(Condition condition, boolean isNested) {
        var builder = ConditionLinkSetMapperParameters.builder()
            .isNested(isNested)
            .linkSetId(messageContext.getIdMapper().getOrNew(ResourceType.Condition, condition.getIdElement().getIdPart()));

        buildEffectiveTimeLow(condition).map(builder::effectiveTimeLow);
        buildEffectiveTimeHigh(condition).map(builder::effectiveTimeHigh);
        buildAvailabilityTime(condition).map(builder::availabilityTime);
        builder.relatedClinicalContent(buildRelatedClinicalContent(condition));

        var qualifier = buildQualifier(condition);
        qualifier.map(value -> value.equalsIgnoreCase(MAJOR)).map(builder::qualifierIsMajor);
        qualifier.map(builder::qualifier);

        var clinicalStatus = buildClinicalStatusCode(condition);
        clinicalStatus.map(value -> value.equalsIgnoreCase(ACTIVE)).map(builder::clinicalStatusIsActive);
        clinicalStatus.map(builder::clinicalStatusCode);

        var conditionNamed = buildConditionNamed(condition);
        if (conditionNamed.isPresent()) {
            conditionNamed
                .filter(value -> !value.equalsIgnoreCase(EXTENSION_NOT_PRESENT))
                .map(builder::conditionNamed);
        } else {
            String newId = messageContext.getIdMapper().getNew();
            builder.generateObservationStatement(true);
            builder.conditionNamed(newId);
            buildPertinentInfo(condition).map(builder::pertinentInfo);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildConditionNamed(Condition condition) {
        Optional<Extension> actualProblemExtension = ExtensionMappingUtils.filterExtensionByUrl(condition, ACTUAL_PROBLEM_URL);
        if (actualProblemExtension.isPresent()) {
            Optional<Reference> reference = actualProblemExtension
                .map(Extension::getValue)
                .map(value -> (Reference) value);


            if (reference.map(this::checkIfReferenceIsObservation).orElse(false)) {
                return reference
                    .map(ref -> messageContext.getIdMapper().getOrNew(ref));
            }
            return Optional.empty();
        } else {
            return Optional.of(EXTENSION_NOT_PRESENT);
        }
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
                .formatDate(condition.getOnsetDateTimeType().getValue()));
        }
        return Optional.empty();
    }

    private Optional<String> buildEffectiveTimeHigh(Condition condition) {
        if (condition.hasAbatement()
            && (condition.hasAbatementDateTimeType() && condition.getAbatementDateTimeType().hasValue())) {
            return Optional.of(DateFormatUtil
                .formatDate(condition.getAbatementDateTimeType().getValue()));
        }
        return Optional.empty();
    }

    private Optional<String> buildAvailabilityTime(Condition condition) {
        if (condition.hasAssertedDate()) {
            return Optional.of(DateFormatUtil
                .formatDate(condition.getAssertedDate()));
        }
        return Optional.empty();
    }

    private List<String> buildRelatedClinicalContent(Condition condition) {
        return ExtensionMappingUtils.filterAllExtensionsByUrl(condition, RELATED_CLINICAL_CONTENT_URL)
           .stream()
           .map(Extension::getValue)
           .map(value -> (Reference) value)
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

    private boolean filterOutListResourceType(Reference reference) {
        return !reference.getReferenceElement().getResourceType().equals(LIST);
    }

    private boolean checkIfReferenceIsObservation(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals(ResourceType.Observation.name());
    }
}
