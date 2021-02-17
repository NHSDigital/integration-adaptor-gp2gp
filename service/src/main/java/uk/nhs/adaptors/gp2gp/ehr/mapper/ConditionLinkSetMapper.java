package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
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
    private static final String RELATED_CLINICAL_CONTENT_COMPONENT_TEMPLATE
        = "<component typeCode=\"COMP\"><statementRef " +
        "classCode=\"OBS\" moodCode=\"EVN\">\r" +
        "<id root=\"%S\"/>\r" +
        "</statementRef>\r" +
        "</component>";
    private static final String CONDITION_NAMED_TEMPLATE = "<conditionNamed typeCode=\"NAME\" inversionInd=\"true\">\r" +
        "<namedStatementRef classCode=\"OBS\" moodCode=\"EVN\">\r" +
        "<id root=\"%S/>\r" +
        "</namedStatementRef>\r" +
        "</conditionNamed>";

    private final MessageContext messageContext;

    public String mapConditionToLinkSet(Condition condition, Bundle bundle, boolean isNested) {
        var builder = LinkSetMapperParameters.builder()
            .isNested(isNested)
            .id(messageContext.getIdMapper().getOrNew(ResourceType.Condition, condition.getId()));

        buildConditionNamed(condition, bundle).map(builder::conditionNamed);
        buildQualifier(condition).map(builder::qualifier);
        buildClinicalStatusCode(condition).map(builder::clinicalStatusCode);
        buildEffectiveTimeLow(condition).map(builder::effectiveTimeLow);
        buildEffectiveTimeHigh(condition).map(builder::effectiveTimeHigh);
        buildAvailabilityTime(condition).map(builder::availabilityTime);
        buildRelatedClinicalContent(condition).map(builder::relatedClinicalContent);

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, builder);
    }

    private Optional<String> buildConditionNamed(Condition condition, Bundle bundle) {
        Optional<Extension> actualProblemExtension = ExtensionMappingUtils.filterExtensionByUrl(condition, ACTUAL_PROBLEM_URL);
        Optional<Reference> reference = actualProblemExtension
            .map(Extension::getValue)
            .map(value -> (Reference) value);

        if (reference.map(this::checkIfReferenceIsObservation).orElse(false)) {
            if (messageContext.getIdMapper().hasBeenMapped(reference.get())) {
                // generate observation statement
            }
        }

        return reference
            .map(Reference::getReferenceElement)
            .map(IIdType::getIdPart)
            .map(id -> String.format(CONDITION_NAMED_TEMPLATE, id));
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

    private Optional<String> buildRelatedClinicalContent(Condition condition) {
       return ExtensionMappingUtils.filterAllExtensionsByUrl(condition, RELATED_CLINICAL_CONTENT_URL)
           .stream()
            .map(Extension::getValue)
            .map(value -> (Reference) value)
            .filter(this::filterOutListResourceType)
            .map(this::populateRelatedClinicalContentTemplate)
            .collect(Collectors
                .collectingAndThen(
                    Collectors.joining(System.lineSeparator()),
                    Optional::of)
            );

    }

    private String populateRelatedClinicalContentTemplate(Reference reference) {
        String id = messageContext.getIdMapper().getOrNew(reference);
        return String.format(RELATED_CLINICAL_CONTENT_COMPONENT_TEMPLATE, id);
    }

    private boolean filterOutListResourceType(Reference reference) {
        return !reference.getReferenceElement().getResourceType().equals("List");
    }

    private boolean checkIfReferenceIsObservation(Reference reference) {
        return reference.getReferenceElement().getResourceType().equals("Observation");
    }

    public static Optional<Resource> extractResourceFromBundle(Bundle bundle, String relativeReference) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> buildRelativeReference(resource).equals(relativeReference))
            .findFirst();
    }
    private static String buildRelativeReference(Resource resource) {
        if (resource.hasIdElement()) {
            IdType idType = resource.getIdElement();
            return idType.getResourceType() + "/" + idType.getIdPart();
        }
        return StringUtils.EMPTY;
    }

    @Getter
    @Setter
    @Builder
    public static class LinkSetMapperParameters {
        private boolean isNested;
        private String id;
        private String conditionNamed;
        private String qualifier;
        private String clinicalStatusCode;
        private String effectiveTimeHigh;
        private String effectiveTimeLow;
        private String availabilityTime;
        private String relatedClinicalContent;
    }
}
