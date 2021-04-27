package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnostic_report;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
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

import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report.SpecimenCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SpecimenMapper {

    private static final Mustache SPECIMEN_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("specimen_compound_statement_template.mustache");
    private static final String FASTING_STATUS_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect"
        + "-FastingStatus-1";
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";
    private static final String DEFAULT_TIME_VALUE = "<center nullFlavor=\"UNK\"/>";

    private final MessageContext messageContext;
    private final ObservationMapper observationMapper;

    public String mapSpecimenToCompoundStatement(Specimen specimen, List<Observation> observations, String diagnosticReportIssuedDate) {
        String mappedObservations = mapObservationsAssociatedWithSpecimen(specimen, observations);

        var specimenCompoundStatementTemplateParameters = SpecimenCompoundStatementTemplateParameters.builder()
            .compoundStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Specimen, specimen.getId()))
            .diagnosticReportIssuedDate(diagnosticReportIssuedDate)
            .effectiveTime(prepareEffectiveTimeForSpecimen(specimen))
            .observations(mappedObservations);

        buildType(specimen).ifPresent(specimenCompoundStatementTemplateParameters::type);
        buildAccessionIdentifier(specimen).ifPresent(specimenCompoundStatementTemplateParameters::accessionIdentifier);
        buildPertinentInformation(specimen).ifPresent(specimenCompoundStatementTemplateParameters::pertinentInformation);
        buildParticipant(specimen).ifPresent(specimenCompoundStatementTemplateParameters::participant);

        return TemplateUtils.fillTemplate(
            SPECIMEN_COMPOUND_STATEMENT_TEMPLATE,
            specimenCompoundStatementTemplateParameters.build()
        );
    }

    public static String prepareEffectiveTimeForSpecimen(Specimen specimen) {
        Optional<DateTimeType> effectiveDate = getCollectionDateTime(specimen);

        return effectiveDate.map(date -> String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, DateFormatUtil.toHl7Format(date)))
            .orElse(DEFAULT_TIME_VALUE);
    }

    private static Optional<DateTimeType> getCollectionDateTime(Specimen specimen) {
        Optional<DateTimeType> effectiveDate = Optional.empty();

        if (specimen.hasReceivedTime()) {
            effectiveDate = Optional.of(specimen.getReceivedTimeElement());
        }

        if (specimen.hasCollection()) {
            Specimen.SpecimenCollectionComponent collection = specimen.getCollection();
            if (collection.hasCollectedDateTimeType()) {
                effectiveDate = Optional.of(collection.getCollectedDateTimeType());
            } else if (collection.hasCollectedPeriod()) {
                effectiveDate = Optional.of(collection.getCollectedPeriod().getStartElement());
            }
        }

        return effectiveDate;
    }

    private Optional<String> buildParticipant(Specimen specimen) {
        if (specimen.hasCollection() && specimen.getCollection().hasCollector()) {
            Reference collector = specimen.getCollection().getCollector();

            return Optional.of(messageContext.getIdMapper().get(collector));
        }

        return Optional.empty();
    }

    private Optional<String> buildAccessionIdentifier(Specimen specimen) {
        if (specimen.hasAccessionIdentifier()) {
            return Optional.of(specimen.getAccessionIdentifier().getValue());
        }

        return Optional.empty();
    }

    private Optional<String> buildType(Specimen specimen) {
        if (specimen.hasType()) {
            return CodeableConceptMappingUtils.extractTextOrCoding(specimen.getType());
        }

        return Optional.empty();
    }

    private String mapObservationsAssociatedWithSpecimen(Specimen specimen, List<Observation> observations) {
        List<Observation> observationsAssociatedWithSpecimen = observations.stream()
            .filter(Observation::hasSpecimen)
            .filter(observation -> observation.getSpecimen().getReference().equals(specimen.getId()))
            .collect(Collectors.toList());

        return observationsAssociatedWithSpecimen.stream().map(
            observationAssociatedWithSpecimen -> observationMapper.mapObservationToCompoundStatement(
                observationAssociatedWithSpecimen,
                observations
            ))
            .collect(Collectors.joining());
    }

    private Optional<String> buildPertinentInformation(Specimen specimen) {
        PertinentInformationBuilder pertinentInformationBuilder = new PertinentInformationBuilder();
        if (specimen.hasCollection()) {
            Specimen.SpecimenCollectionComponent collection = specimen.getCollection();

            collection.getExtensionsByUrl(FASTING_STATUS_URL).stream().findFirst()
                .ifPresent(extension -> {
                    Type value = extension.getValue();
                    if (value instanceof CodeableConcept) {
                        pertinentInformationBuilder.fastingStatus((CodeableConcept) value);
                    } else if (value instanceof Duration) {
                        pertinentInformationBuilder.fastingDuration((Duration) value);
                    }
                });

            if (collection.hasQuantity()) {
                pertinentInformationBuilder.quantity(collection.getQuantity());
            }

            if (collection.hasBodySite()) {
                pertinentInformationBuilder.collectionSite(collection.getBodySite());
            }
        }

        specimen.getNote().forEach(note -> pertinentInformationBuilder.note(note.getText()));

        return getCollectionDateTime(specimen)
            .map(pertinentInformationBuilder::build)
            .orElseGet(pertinentInformationBuilder::build);
    }

    private static class PertinentInformationBuilder {

        private static final String FASTING_STATUS = "Fasting Status:";
        private static final String FASTING_DURATION = "Fasting Duration:";
        private static final String QUANTITY = "Quantity:";
        private static final String COLLECTION_SITE = "Collection Site:";
        private static final String COMMENT_PREFIX = "comment type - LAB SPECIMEN COMMENT(E271)";

        private String pertinentInformation;

        PertinentInformationBuilder() {
            pertinentInformation = StringUtils.EMPTY;
        }

        public void fastingStatus(CodeableConcept fastingStatus) {
            CodeableConceptMappingUtils.extractTextOrCoding(fastingStatus)
                .map(fastingStatusValue -> newLine(withSpace(FASTING_STATUS, fastingStatusValue), pertinentInformation))
                .ifPresent(text -> pertinentInformation = text);
        }

        public void fastingDuration(Duration fastingDuration) {
            pertinentInformation = newLine(
                withSpace(FASTING_DURATION, fastingDuration.getValue().toString(), fastingDuration.getUnit()),
                pertinentInformation);
        }

        public void quantity(SimpleQuantity quantity) {
            pertinentInformation = newLine(
                withSpace(QUANTITY, quantity.getValue().toString(), quantity.getUnit()),
                pertinentInformation);
        }

        public void collectionSite(CodeableConcept collectionSite) {
            CodeableConceptMappingUtils.extractTextOrCoding(collectionSite)
                .map(collectionSiteValue -> newLine(withSpace(COLLECTION_SITE, collectionSiteValue), pertinentInformation))
                .ifPresent(text -> pertinentInformation = text);
        }

        public void note(String note) {
            pertinentInformation = newLine(note, pertinentInformation);
        }

        public Optional<String> build(DateTimeType date) {
            if (StringUtils.isNotBlank(pertinentInformation)) {
                return Optional.of(newLine(withSpace(COMMENT_PREFIX, DateFormatUtil.toTextFormat(date)), pertinentInformation));
            }

            return Optional.empty();
        }

        public Optional<String> build() {
            if (StringUtils.isNotBlank(pertinentInformation)) {
                return Optional.of(newLine(COMMENT_PREFIX, pertinentInformation));
            }

            return Optional.empty();
        }

        private String newLine(String text, String pertinentInformation) {
            return StringUtils.joinWith(
                StringUtils.LF,
                text,
                pertinentInformation);
        }

        private String withSpace(Object... values) {
            return StringUtils.joinWith(StringUtils.SPACE, values);
        }
    }
}
