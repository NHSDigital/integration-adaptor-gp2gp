package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper.DUMMY_SPECIMEN_ID_PREFIX;
import static uk.nhs.adaptors.gp2gp.ehr.utils.TextUtils.newLine;
import static uk.nhs.adaptors.gp2gp.ehr.utils.TextUtils.withSpace;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Duration;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.Specimen;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.SpecimenCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SpecimenMapper {

    private static final Mustache SPECIMEN_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("specimen_compound_statement_template.mustache");

    private static final String FASTING_STATUS_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect"
        + "-FastingStatus-1";
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";

    private final MessageContext messageContext;
    private final ObservationMapper observationMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapSpecimenToCompoundStatement(Specimen specimen, List<Observation> observations, String diagnosticReportIssuedDate) {
        String mappedObservations = mapObservationsAssociatedWithSpecimen(specimen, observations);

        var specimenCompoundStatementTemplateParameters = SpecimenCompoundStatementTemplateParameters.builder()
            .compoundStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Specimen, specimen.getIdElement()))
            .availabilityTime(diagnosticReportIssuedDate)
            .specimenRoleId(randomIdGeneratorService.createNewId())
            .narrativeStatementId(randomIdGeneratorService.createNewId())
            .observations(mappedObservations);

        buildAccessionIdentifier(specimen).ifPresent(specimenCompoundStatementTemplateParameters::accessionIdentifier);
        buildEffectiveTimeForSpecimen(specimen).ifPresent(specimenCompoundStatementTemplateParameters::effectiveTime);
        buildSpecimenMaterialType(specimen).ifPresent(specimenCompoundStatementTemplateParameters::specimenMaterialType);
        buildNarrativeStatement(specimen, isEmpty(mappedObservations))
            .ifPresent(specimenCompoundStatementTemplateParameters::narrativeStatement);
        buildParticipant(specimen).ifPresent(specimenCompoundStatementTemplateParameters::participant);

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

    private Optional<String> buildParticipant(Specimen specimen) {
        if (specimen.hasCollection() && specimen.getCollection().hasCollector()) {
            Reference collector = specimen.getCollection().getCollector();

            return Optional.of(messageContext.getIdMapper().get(collector));
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

    private String mapObservationsAssociatedWithSpecimen(Specimen specimen, List<Observation> observations) {
        List<Observation> observationsAssociatedWithSpecimen;

        if (specimen.getIdElement().getIdPart().contains(DUMMY_SPECIMEN_ID_PREFIX)) {
            observationsAssociatedWithSpecimen = observations;
        } else {
            observationsAssociatedWithSpecimen = observations.stream()
                .filter(Observation::hasSpecimen)
                .filter(observation -> observation.getSpecimen().getReference().equals(specimen.getId()))
                .collect(Collectors.toList());
        }

        return observationsAssociatedWithSpecimen.stream()
            .map(observationMapper::mapObservationToCompoundStatement)
            .collect(Collectors.joining());
    }

    private Optional<String> buildNarrativeStatement(Specimen specimen, boolean generateDummyStatement) {
        NarrativeStatementBuilder narrativeStatementBuilder = new NarrativeStatementBuilder(generateDummyStatement);
        if (specimen.hasCollection()) {
            Specimen.SpecimenCollectionComponent collection = specimen.getCollection();

            collection.getExtensionsByUrl(FASTING_STATUS_URL).stream().findFirst()
                .ifPresent(extension -> {
                    Type value = extension.getValue();
                    if (value instanceof CodeableConcept) {
                        narrativeStatementBuilder.fastingStatus((CodeableConcept) value);
                    } else if (value instanceof Duration) {
                        narrativeStatementBuilder.fastingDuration((Duration) value);
                    }
                });

            if (collection.hasQuantity()) {
                narrativeStatementBuilder.quantity(collection.getQuantity());
            }

            if (collection.hasBodySite()) {
                narrativeStatementBuilder.collectionSite(collection.getBodySite());
            }
        }

        specimen.getNote().stream()
            .map(Annotation::getText)
            .forEach(narrativeStatementBuilder::note);

        return getEffectiveTime(specimen)
            .map(narrativeStatementBuilder::buildWithDateTime)
            .orElseGet(narrativeStatementBuilder::build);
    }

    private static class NarrativeStatementBuilder {

        private static final String FASTING_STATUS = "Fasting Status:";
        private static final String FASTING_DURATION = "Fasting Duration:";
        private static final String QUANTITY = "Quantity:";
        private static final String COLLECTION_SITE = "Collection Site:";
        private static final String COMMENT_PREFIX = "comment type - LAB SPECIMEN COMMENT(E271)";

        private String text;
        private boolean generateDummyStatement;

        NarrativeStatementBuilder(boolean generateDummyStatement) {
            this.generateDummyStatement = generateDummyStatement;
            text = StringUtils.EMPTY;
        }

        private void prependComment(String... texts) {
            text = newLine(withSpace((Object[]) texts), text);
        }

        private void prependComment(List<String> texts) {
            text = newLine(withSpace(texts), text);
        }

        public void fastingStatus(CodeableConcept fastingStatus) {
            CodeableConceptMappingUtils.extractTextOrCoding(fastingStatus)
                .ifPresent(fastingStatusValue -> prependComment(FASTING_STATUS, fastingStatusValue));
        }

        public void fastingDuration(Duration fastingDuration) {
            List<String> fastingDurationElements = List.of(
                FASTING_DURATION,
                Objects.toString(fastingDuration.getValue(), StringUtils.EMPTY),
                fastingDuration.getUnit()
            );

            prependComment(fastingDurationElements);
        }

        public void quantity(SimpleQuantity quantity) {
            List<String> quantityElements = List.of(
                QUANTITY,
                Objects.toString(quantity.getValue(), StringUtils.EMPTY),
                quantity.getUnit()
            );

            prependComment(quantityElements);
        }

        public void collectionSite(CodeableConcept collectionSite) {
            CodeableConceptMappingUtils.extractTextOrCoding(collectionSite)
                .ifPresent(collectionSiteValue -> prependComment(COLLECTION_SITE, collectionSiteValue));
        }

        public void note(String note) {
            prependComment(note);
        }

        public Optional<String> buildWithDateTime(DateTimeType date) {
            if (StringUtils.isNotBlank(text)) {
                prependComment(COMMENT_PREFIX, DateFormatUtil.toTextFormat(date));
                return Optional.of(text);
            } else if (generateDummyStatement) {
                prependComment("CommentType:" + CommentType.AGGREGATE_COMMENT_SET.getCode(),
                    "CommentDate: " + DateFormatUtil.toTextFormat(date),
                    "EMPTY REPORT");
                return Optional.of(text);
            } else {
                return Optional.empty();
            }
        }

        public Optional<String> build() {
            if (StringUtils.isNotBlank(text)) {
                prependComment(COMMENT_PREFIX);
                return Optional.of(text);
            } else if (generateDummyStatement) {
                prependComment("CommentType:" + CommentType.AGGREGATE_COMMENT_SET.getCode(), "EMPTY REPORT");
                return Optional.of(text);
            } else {
                return Optional.empty();
            }
        }
    }
}
