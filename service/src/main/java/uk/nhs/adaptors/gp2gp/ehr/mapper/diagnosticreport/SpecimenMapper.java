package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper.NARRATIVE_STATEMENT_TEMPLATE;
import static uk.nhs.adaptors.gp2gp.ehr.utils.TextUtils.newLine;
import static uk.nhs.adaptors.gp2gp.ehr.utils.TextUtils.withSpace;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Duration;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.Specimen;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.SpecimenCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SpecimenMapper {

    private static final Mustache SPECIMEN_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("specimen_compound_statement_template.mustache");

    private static final String FASTING_STATUS_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect"
        + "-FastingStatus-1";
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";
    private static final String EMPTY_SPECIMEN_STRING = "EMPTY SPECIMEN";

    private final MessageContext messageContext;
    private final ObservationMapper observationMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final ConfidentialityService confidentialityService;

    public String mapSpecimenToCompoundStatement(
            Specimen specimen, List<Observation> observationsAssociatedWithSpecimen, DiagnosticReport diagnosticReport
    ) {
        String availabilityTimeElement = StatementTimeMappingUtils.prepareAvailabilityTime(diagnosticReport.getIssuedElement());
        String mappedObservations = mapObservationsToCompoundStatements(observationsAssociatedWithSpecimen);

        var specimenCompoundStatementTemplateParameters = SpecimenCompoundStatementTemplateParameters.builder()
            .compoundStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Specimen, specimen.getIdElement()))
            .availabilityTimeElement(availabilityTimeElement)
            .specimenRoleId(randomIdGeneratorService.createNewId())
            .narrativeStatementId(randomIdGeneratorService.createNewId())
            .observations(mappedObservations)
            .confidentialityCode(confidentialityService.generateConfidentialityCode(specimen).orElse(null));

        buildAccessionIdentifier(specimen).ifPresent(specimenCompoundStatementTemplateParameters::accessionIdentifier);
        buildEffectiveTimeForSpecimen(specimen).ifPresent(specimenCompoundStatementTemplateParameters::effectiveTime);
        buildSpecimenMaterialType(specimen).ifPresent(specimenCompoundStatementTemplateParameters::specimenMaterialType);
        buildSpecimenNarrativeStatement(specimen, availabilityTimeElement, mappedObservations.isEmpty())
            .ifPresent(specimenCompoundStatementTemplateParameters::narrativeStatement);

        return TemplateUtils.fillTemplate(
            SPECIMEN_COMPOUND_STATEMENT_TEMPLATE,
            specimenCompoundStatementTemplateParameters.build()
        );
    }

    private Optional<String> buildEffectiveTimeForSpecimen(Specimen specimen) {
        return getEffectiveTime(specimen)
            .map(DateFormatUtil::toHl7Format)
            .map(date -> String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, date));
    }

    private Optional<DateTimeType> getEffectiveTime(Specimen specimen) {
        return getCollectionDate(specimen).or(() -> getReceivedTime(specimen));
    }

    private Optional<DateTimeType> getReceivedTime(Specimen specimen) {
        if (specimen.hasReceivedTime()) {
            return Optional.of(specimen.getReceivedTimeElement());
        }

        return Optional.empty();
    }

    private Optional<DateTimeType> getCollectionDate(Specimen specimen) {
        if (specimen.hasCollection()) {
            Specimen.SpecimenCollectionComponent collection = specimen.getCollection();
            if (collection.hasCollectedDateTimeType()) {
                return Optional.of(collection.getCollectedDateTimeType());
            } else if (collection.hasCollectedPeriod() && collection.getCollectedPeriod().hasStartElement()) {
                return Optional.of(collection.getCollectedPeriod().getStartElement());
            }
        }

        return Optional.empty();
    }

    private Optional<String> buildAccessionIdentifier(Specimen specimen) {
        if (specimen.hasAccessionIdentifier() && specimen.getAccessionIdentifier().hasValue()) {
            return Optional.of(specimen.getAccessionIdentifier().getValue());
        }

        return Optional.empty();
    }

    private Optional<String> buildSpecimenMaterialType(Specimen specimen) {
        if (specimen.hasType()) {
            return CodeableConceptMappingUtils.extractTextOrCoding(specimen.getType());
        }

        return Optional.empty();
    }

    private String mapObservationsToCompoundStatements(List<Observation> observations) {
        return observations.stream()
            .map(observationMapper::mapObservationToCompoundStatement)
            .collect(Collectors.joining());
    }

    private Optional<String> buildSpecimenNarrativeStatement(Specimen specimen, String availabilityTimeElement,
                                                             Boolean emptyMappedObservations) {
        SpecimenNarrativeStatementCommentBuilder specimenNarrativeStatementCommentBuilder = new SpecimenNarrativeStatementCommentBuilder();

        if (emptyMappedObservations) {
            specimenNarrativeStatementCommentBuilder.appendText(EMPTY_SPECIMEN_STRING);
        }

        getReceivedTime(specimen)
            .map(PrimitiveType::getValue)
            .ifPresent(specimenNarrativeStatementCommentBuilder::receivedDate);

        if (specimen.hasCollection()) {
            Specimen.SpecimenCollectionComponent collection = specimen.getCollection();

            collection.getExtensionsByUrl(FASTING_STATUS_URL).stream().findFirst()
                .ifPresent(extension -> {
                    Type value = extension.getValue();
                    if (value instanceof CodeableConcept codeableConcept) {
                        specimenNarrativeStatementCommentBuilder.fastingStatus(codeableConcept);
                    } else if (value instanceof Duration duration) {
                        specimenNarrativeStatementCommentBuilder.fastingDuration(duration);
                    }
                });

            if (collection.hasQuantity()) {
                specimenNarrativeStatementCommentBuilder.quantity(collection.getQuantity());
            }

            if (collection.hasBodySite()) {
                specimenNarrativeStatementCommentBuilder.collectionSite(collection.getBodySite());
            }

            if (collection.hasCollector()) {
                messageContext.getInputBundleHolder()
                    .getResource(collection.getCollector().getReferenceElement())
                    .filter(this::isPractitionerResource)
                    .map(Practitioner.class::cast)
                    .filter(Practitioner::hasName)
                    .map(SpecimenMapper::buildHumanName)
                    .ifPresent(specimenNarrativeStatementCommentBuilder::collector);
            }
        }

        specimen.getNote().stream()
            .map(Annotation::getText)
            .forEach(specimenNarrativeStatementCommentBuilder::note);

        if (StringUtils.isNotBlank(specimenNarrativeStatementCommentBuilder.text)) {
            var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
                .narrativeStatementId(randomIdGeneratorService.createNewId())
                .commentType(CommentType.LAB_SPECIMEN_COMMENT.getCode())
                .comment(specimenNarrativeStatementCommentBuilder.text)
                .availabilityTimeElement(availabilityTimeElement);

            getEffectiveTime(specimen)
                .map(DateFormatUtil::toHl7Format)
                .ifPresent(narrativeStatementTemplateParameters::commentDate);

            return Optional.ofNullable(
                TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build())
            );
        }

        return Optional.empty();
    }

    private boolean isPractitionerResource(Resource resource) {
        return ResourceType.Practitioner.equals(resource.getResourceType());
    }

    private static String buildHumanName(Practitioner practitioner) {
        var practitionerName = practitioner.getNameFirstRep();
        return Stream.of(
                practitionerName.getPrefixAsSingleString(),
                practitionerName.getGivenAsSingleString(),
                practitionerName.getFamily())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private static class SpecimenNarrativeStatementCommentBuilder {

        private static final String FASTING_STATUS = "Fasting Status:";
        private static final String FASTING_DURATION = "Fasting Duration:";
        private static final String QUANTITY = "Quantity:";
        private static final String COLLECTION_SITE = "Collection Site:";
        private static final String COLLECTED_BY = "Collected By:";
        private static final String RECEIVED_DATE = "Received Date:";

        private String text;

        SpecimenNarrativeStatementCommentBuilder() {
            text = StringUtils.EMPTY;
        }

        private void prependText(String... texts) {
            text = newLine(withSpace((Object[]) texts), text);
        }

        private void appendText(String appendText) {
            text = newLine(text, appendText);
        }

        private void prependText(List<String> texts) {
            text = newLine(withSpace(texts), text);
        }

        public void fastingStatus(CodeableConcept fastingStatus) {
            CodeableConceptMappingUtils.extractTextOrCoding(fastingStatus)
                .ifPresent(fastingStatusValue -> prependText(FASTING_STATUS, fastingStatusValue));
        }

        public void fastingDuration(Duration fastingDuration) {
            List<String> fastingDurationElements = List.of(
                FASTING_DURATION,
                Objects.toString(fastingDuration.getValue(), StringUtils.EMPTY),
                fastingDuration.getUnit()
            );

            prependText(fastingDurationElements);
        }

        public void quantity(SimpleQuantity quantity) {
            List<String> quantityElements = List.of(
                QUANTITY,
                Objects.toString(quantity.getValue(), StringUtils.EMPTY),
                quantity.getUnit()
            );

            prependText(quantityElements);
        }

        public void receivedDate(Date date) {
            List<String> receivedDateElements = List.of(
                RECEIVED_DATE,
                Objects.toString(DateFormatUtil.toTextFormatStraight(date), StringUtils.EMPTY)
            );

            prependText(receivedDateElements);
        }

        public void collectionSite(CodeableConcept collectionSite) {
            CodeableConceptMappingUtils.extractTextOrCoding(collectionSite)
                .ifPresent(collectionSiteValue -> prependText(COLLECTION_SITE, collectionSiteValue));
        }

        public void collector(String collectorName) {
            appendText(COLLECTED_BY + StringUtils.SPACE + collectorName);
        }

        public void note(String note) {
            prependText(note);
        }
    }
}
