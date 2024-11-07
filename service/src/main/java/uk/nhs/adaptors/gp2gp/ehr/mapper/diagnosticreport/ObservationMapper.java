package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper.DUMMY_OBSERVATION_ID_PREFIX;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationRelatedComponent;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CompoundStatementClassCode;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.ObservationCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.ObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ObservationMapper {

    public static final Mustache NARRATIVE_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("narrative_statement_template.mustache");

    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_statement_template.mustache");
    private static final Mustache OBSERVATION_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_compound_statement_template.mustache");

    private static final String INTERPRETATION_CODE = "<interpretationCode code=";

    private static final String DATA_ABSENT_PREFIX = "Data Absent: ";
    private static final String VALUE_PREFIX = "Value: ";
    private static final String INTERPRETATION_PREFIX = "Interpretation: ";
    private static final String BODY_SITE_PREFIX = "Site: ";
    private static final String METHOD_PREFIX = "Method: ";
    private static final String RANGE_UNITS_PREFIX = "Range Units: ";
    private static final String RANGE_PREFIX = "Range: ";
    private static final String RANGE_TEXT_PREFIX = "Range Text: ";

    private static final String RANGE_LOW_PREFIX = "Low: ";
    private static final String RANGE_HIGH_PREFIX = "High: ";

    private static final String HL7_UNKNOWN_VALUE = "UNK";
    private static final String HAS_MEMBER_TYPE = "HASMEMBER";

    private static final List<Class<? extends Type>> UNHANDLED_TYPES = List.of(SampledData.class, Attachment.class);

    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";
    private static final Set<String> INTERPRETATION_CODES = Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA");

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final ConfidentialityService confidentialityService;

    public String mapObservationToCompoundStatement(Observation observationAssociatedWithSpecimen) {
        return mapAndVerify(createHolder(observationAssociatedWithSpecimen));
    }

    private String mapAndVerify(MultiStatementObservationHolder observationAssociatedWithSpecimen) {
        var relatedObservations = getRelatedObservations(observationAssociatedWithSpecimen);

        final String output;
        if (relatedObservations.isEmpty()
            && (!hasDiagnosticReportResultReference(observationAssociatedWithSpecimen)
            || !willHaveNarrativeStatements(observationAssociatedWithSpecimen))
        ) {
            output = outputWithoutCompoundStatement(observationAssociatedWithSpecimen);
        } else {
            output = outputWithCompoundStatement(observationAssociatedWithSpecimen, relatedObservations);
        }

        observationAssociatedWithSpecimen.verifyObservationWasMapped();
        relatedObservations.forEach(MultiStatementObservationHolder::verifyObservationWasMapped);
        return output;
    }

    private boolean willHaveNarrativeStatements(MultiStatementObservationHolder holder) {
        var futureNarrativeStatement = prepareNarrativeStatements(
            holder,
            isInterpretationCodeMapped(
                prepareObservationStatement(holder, CompoundStatementClassCode.CLUSTER).orElse(StringUtils.EMPTY)
            ));

        return futureNarrativeStatement.isPresent() && !futureNarrativeStatement.get().isEmpty();
    }

    private boolean hasDiagnosticReportResultReference(MultiStatementObservationHolder observationHolder) {
        return messageContext.getInputBundleHolder().getResourcesOfType(DiagnosticReport.class)
            .stream()
            .map(DiagnosticReport.class::cast)
            .anyMatch(diagnosticReport ->
                diagnosticReport.getResult()
                    .stream()
                    .anyMatch(reference -> observationHolder.getObservation().getId().equals(reference.getReference()))
            );
    }

    private String outputWithCompoundStatement(MultiStatementObservationHolder observationAssociatedWithSpecimen,
        List<MultiStatementObservationHolder> relatedObservations) {
        String compoundStatementId = observationAssociatedWithSpecimen.nextHl7InstanceIdentifier();
        var observation = observationAssociatedWithSpecimen.getObservation();
        String codeElement = prepareCodeElement(observation);
        String effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation);
        String availabilityTimeElement =
            StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observation);
        CompoundStatementClassCode classCode = prepareClassCode(relatedObservations);

        String observationStatement = prepareObservationStatement(observationAssociatedWithSpecimen, classCode)
            .orElse(StringUtils.EMPTY);
        String narrativeStatements = prepareNarrativeStatements(
            observationAssociatedWithSpecimen,
            isInterpretationCodeMapped(observationStatement))
            .orElse(StringUtils.EMPTY);
        String statementsForDerivedObservations = prepareStatementsForDerivedObservations(relatedObservations);

        var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
            .classCode(classCode.getCode())
            .compoundStatementId(compoundStatementId)
            .codeElement(codeElement)
            .effectiveTime(effectiveTime)
            .availabilityTimeElement(availabilityTimeElement)
            .confidentialityCode(confidentialityService.generateConfidentialityCode(observation).orElse(null))
            .observationStatement(observationStatement)
            .narrativeStatements(narrativeStatements)
            .statementsForDerivedObservations(statementsForDerivedObservations);

        return TemplateUtils.fillTemplate(
            OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
            observationCompoundStatementTemplateParameters.build()
        );
    }

    private List<MultiStatementObservationHolder> getRelatedObservations(
        MultiStatementObservationHolder observationAssociatedWithSpecimen) {
        return observationAssociatedWithSpecimen.getObservation().getRelated().stream()
            .filter(observationRelation -> observationRelation.getType() == Observation.ObservationRelationshipType.HASMEMBER)
            .map(ObservationRelatedComponent::getTarget)
            .map(Reference::getReferenceElement)
            .map(referenceElement -> messageContext.getInputBundleHolder().getResource(referenceElement))
            .flatMap(Optional::stream)
            .map(Observation.class::cast)
            .map(observation -> createHolder(observation))
            .collect(Collectors.toList());
    }

    private String outputWithoutCompoundStatement(MultiStatementObservationHolder observationAssociatedWithSpecimen) {
        CompoundStatementClassCode classCode = CompoundStatementClassCode.CLUSTER;

        String observationStatement = prepareObservationStatement(observationAssociatedWithSpecimen, classCode)
            .orElse(StringUtils.EMPTY);

        String narrativeStatements = prepareNarrativeStatements(
            observationAssociatedWithSpecimen,
            isInterpretationCodeMapped(observationStatement))
            .orElse(StringUtils.EMPTY);

        return observationStatement + narrativeStatements;
    }

    private Optional<String> prepareObservationStatement(
        MultiStatementObservationHolder observation, CompoundStatementClassCode classCode) {
        if (observationHasNonCommentNoteCode(observation.getObservation()) && classCode.equals(CompoundStatementClassCode.CLUSTER)) {
            return Optional.of(mapObservationToObservationStatement(observation));
        }

        return Optional.empty();
    }

    private String mapObservationToObservationStatement(MultiStatementObservationHolder holder) {
        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(holder.nextHl7InstanceIdentifier())
            .codeElement(prepareCodeElement(holder.getObservation()))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(holder.getObservation()))
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(holder.getObservation()))
            .confidentialityCode(confidentialityService.generateConfidentialityCode(holder.getObservation()).orElse(null));

        if (holder.getObservation().hasValue() && !holder.getObservation().hasValueStringType()) {
            Type value = holder.getObservation().getValue();

            if (UNHANDLED_TYPES.contains(value.getClass())) {
                throw new EhrMapperException(
                    String.format("Observation value type %s not supported.", holder.getObservation().getValue().getClass()));
            } else if (structuredObservationValueMapper.isStructuredValueType(value)) {
                observationStatementTemplateParametersBuilder.value(
                    structuredObservationValueMapper.mapObservationValueToStructuredElement(value)
                );
            }
        }

        if (holder.getObservation().hasReferenceRange() && holder.getObservation().hasValueQuantity()) {
            observationStatementTemplateParametersBuilder.referenceRange(
                structuredObservationValueMapper.mapReferenceRangeTypeForDiagnosticReport(holder.getObservation()
                        .getReferenceRangeFirstRep()));
        }

        prepareInterpretation(holder.getObservation()).ifPresent(observationStatementTemplateParametersBuilder::interpretation);

        return TemplateUtils.fillTemplate(
            OBSERVATION_STATEMENT_TEMPLATE,
            observationStatementTemplateParametersBuilder.build()
        );
    }

    private Optional<String> prepareNarrativeStatements(MultiStatementObservationHolder holder, boolean interpretationCodeMapped) {
        Observation observation = holder.getObservation();

        if (observation.getIdElement().getIdPart().contains(DUMMY_OBSERVATION_ID_PREFIX)) {
            return Optional.of(
                mapObservationToNarrativeStatement(holder, observation.getComment(), CommentType.AGGREGATE_COMMENT_SET.getCode())
            );
        }

        StringBuilder narrativeStatementsBlock = new StringBuilder();

        CodeableConceptMappingUtils.extractTextOrCoding(observation.getDataAbsentReason())
            .map(DATA_ABSENT_PREFIX::concat)
            .map(comment ->
                mapObservationToNarrativeStatement(holder, comment, CommentType.AGGREGATE_COMMENT_SET.getCode())
            )
            .ifPresent(narrativeStatementsBlock::append);

        StringBuilder interpretationTextAndComment = new StringBuilder();

        if (!interpretationCodeMapped && observation.hasInterpretation()) {
            CodeableConceptMappingUtils.extractUserSelectedTextOrCoding(observation.getInterpretation()).ifPresent(interpretationText ->
                interpretationTextAndComment.append(INTERPRETATION_PREFIX).append(interpretationText));
        }

        if (observation.hasComment()) {
            interpretationTextAndComment.append(StringUtils.LF).append(observation.getComment());
        }

        if (observation.hasValueStringType()) {
            interpretationTextAndComment.append(StringUtils.LF).append(VALUE_PREFIX).append(observation.getValueStringType());
        }

        if (observation.hasReferenceRange() && observation.getReferenceRangeFirstRep().hasText()) {
            interpretationTextAndComment.append(StringUtils.LF).append(RANGE_TEXT_PREFIX)
                    .append(observation.getReferenceRangeFirstRep().getText());
        }

        CodeableConceptMappingUtils.extractTextOrCoding(observation.getBodySite())
            .map(BODY_SITE_PREFIX::concat)
            .map(comment ->
                mapObservationToNarrativeStatement(holder, comment, CommentType.AGGREGATE_COMMENT_SET.getCode())
            )
            .ifPresent(narrativeStatementsBlock::append);

        CodeableConceptMappingUtils.extractTextOrCoding(observation.getMethod())
            .map(METHOD_PREFIX::concat)
            .map(comment ->
                mapObservationToNarrativeStatement(holder, comment, CommentType.AGGREGATE_COMMENT_SET.getCode())
            )
            .ifPresent(narrativeStatementsBlock::append);

        if (observation.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent referenceRange = observation.getReferenceRangeFirstRep();

            if (observation.hasValueQuantity()) {
                extractUnit(referenceRange)
                    .filter(referenceRangeUnit -> isRangeUnitValid(referenceRangeUnit, observation.getValueQuantity()))
                    .map(RANGE_UNITS_PREFIX::concat)
                    .map(StringUtils.LF::concat)
                    .ifPresent(interpretationTextAndComment::append);
            } else {
                interpretationTextAndComment
                    .append(StringUtils.LF)
                    .append(prepareReferenceRangeToComment(referenceRange));
            }
        }

        Optional.of(interpretationTextAndComment)
            .map(StringBuilder::toString)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .map(textAndComment ->
                mapObservationToNarrativeStatement(
                    holder, textAndComment, prepareCommentType(observation).getCode())
            ).ifPresent(narrativeStatementsBlock::append);

        if (!narrativeStatementsBlock.toString().isBlank()) {
            return Optional.of(narrativeStatementsBlock.toString());
        }

        return Optional.empty();
    }

    private Optional<String> prepareNarrativeStatementForRelatedObservationComments(MultiStatementObservationHolder holder) {
        Observation observation = holder.getObservation();

        StringBuilder relatedObservationsComments = new StringBuilder();
        if (observation.hasRelated()) {
            List<Observation> validObservations = observation.getRelated()
                .stream()
                .filter(this::hasValidType)
                .map(observationRelatedComponent -> observationRelatedComponent.getTarget().getResource())
                .map(Observation.class::cast)
                .filter(this::hasValidComment)
                .collect(Collectors.toList());

            if (!validObservations.isEmpty()) {
                final IdMapper idMapper = messageContext.getIdMapper();

                validObservations.forEach(validObservation -> {
                    idMapper.markObservationAsMapped(validObservation.getIdElement());
                    relatedObservationsComments.append(StringUtils.LF).append(validObservation.getComment());
                });
            }

            StringBuilder narrativeStatementsBlock = new StringBuilder();

            Optional.of(relatedObservationsComments)
                .map(StringBuilder::toString)
                .filter(StringUtils::isNotBlank)
                .map(textAndComment -> mapObservationToNarrativeStatement(
                        holder, textAndComment, CommentType.USER_COMMENT.getCode())
                ).ifPresent(narrativeStatementsBlock::append);

            if (!narrativeStatementsBlock.toString().isBlank()) {
                return Optional.of(narrativeStatementsBlock.toString());
            }
        }

        return Optional.empty();
    }

    private CommentType prepareCommentType(Observation observation) {
        return DiagnosticReportMapper.isFilingComment(observation)
            ? CommentType.USER_COMMENT : CommentType.AGGREGATE_COMMENT_SET;
    }

    private String prepareReferenceRangeValues(SimpleQuantity simpleQuantity) {
        return Stream.of(
                Optional.ofNullable(simpleQuantity.getValue()),
                Optional.ofNullable(simpleQuantity.getUnit())
            )
            .flatMap(Optional::stream)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String prepareReferenceRangeToComment(Observation.ObservationReferenceRangeComponent referenceRange) {
        StringBuilder referenceRangeCommentLine = new StringBuilder(RANGE_PREFIX);
        if (referenceRange.hasLow()) {
            referenceRangeCommentLine.append(
                RANGE_LOW_PREFIX.concat(prepareReferenceRangeValues(referenceRange.getLow()))
            ).append(StringUtils.SPACE);
        }

        if (referenceRange.hasHigh()) {
            referenceRangeCommentLine.append(
                RANGE_HIGH_PREFIX.concat(prepareReferenceRangeValues(referenceRange.getHigh()))
            );
        }

        return referenceRangeCommentLine.toString().trim();
    }

    private String mapObservationToNarrativeStatement(MultiStatementObservationHolder holder, String comment, String commentType) {
        var observation = holder.getObservation();
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(holder.nextHl7InstanceIdentifier())
            .commentType(commentType)
            .commentDate(handleEffectiveToCommentDate(observation))
            .comment(comment)
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observation))
            .confidentialityCode(confidentialityService.generateConfidentialityCode(observation).orElse(null));

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build());
    }

    private String handleEffectiveToCommentDate(Observation observation) {
        if (observation.hasEffective()) {
            if (observation.hasEffectiveDateTimeType()) {
                return DateFormatUtil.toHl7Format(observation.getEffectiveDateTimeType());
            } else if (observation.hasEffectivePeriod() && observation.getEffectivePeriod().hasStart()) {
                return DateFormatUtil.toHl7Format(observation.getEffectivePeriod().getStartElement());
            }
        }
        return HL7_UNKNOWN_VALUE;
    }

    private String prepareStatementsForDerivedObservations(List<MultiStatementObservationHolder> derivedObservations) {
        StringBuilder derivedObservationsBlock = new StringBuilder();

        derivedObservations.forEach(derivedObservationHolder -> {
            var derivedObservation = derivedObservationHolder.getObservation();
            Optional<String> observationStatement =
                prepareObservationStatement(derivedObservationHolder, CompoundStatementClassCode.CLUSTER);
            Optional<String> narrativeStatements = prepareNarrativeStatements(
                derivedObservationHolder,
                isInterpretationCodeMapped(observationStatement.orElse(StringUtils.EMPTY)));
            Optional<String> relatedObservationNarrativeStatement = prepareNarrativeStatementForRelatedObservationComments(
                derivedObservationHolder);

            if (relatedObservationNarrativeStatement.isPresent()) {
                if (narrativeStatements.isPresent()) {
                    narrativeStatements = Optional.of(narrativeStatements.get().concat(relatedObservationNarrativeStatement.get()));
                } else {
                    narrativeStatements = relatedObservationNarrativeStatement;
                }
            }

            if (observationStatement.isPresent() && narrativeStatements.isPresent()) {
                String compoundStatementId = derivedObservationHolder.nextHl7InstanceIdentifier();
                String codeElement = prepareCodeElement(derivedObservation);
                String effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForObservation(derivedObservation);
                String availabilityTimeElement =
                    StatementTimeMappingUtils.prepareAvailabilityTime(resolveEffectiveDateTimeType(derivedObservation));

                var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
                    .classCode(CompoundStatementClassCode.CLUSTER.getCode())
                    .compoundStatementId(compoundStatementId)
                    .codeElement(codeElement)
                    .effectiveTime(effectiveTime)
                    .availabilityTimeElement(availabilityTimeElement)
                    .observationStatement(observationStatement.get())
                    .narrativeStatements(narrativeStatements.get());
                derivedObservationsBlock.append(
                    TemplateUtils.fillTemplate(
                        OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
                        observationCompoundStatementTemplateParameters.build()
                    )
                );
            } else {
                observationStatement.ifPresent(derivedObservationsBlock::append);
                narrativeStatements.ifPresent(derivedObservationsBlock::append);
            }
        });

        return derivedObservationsBlock.toString();
    }

    private DateTimeType resolveEffectiveDateTimeType(Observation observation) {
        if (observation.hasEffective()) {
            if (observation.hasEffectiveDateTimeType()) {
                return observation.getEffectiveDateTimeType();
            } else if (observation.hasEffectivePeriod() && observation.getEffectivePeriod().hasStart()) {
                return observation.getEffectivePeriod().getStartElement();
            }
        }
        return new DateTimeType();
    }

    private boolean observationHasNonCommentNoteCode(Observation observation) {
        return observation.hasCode() && !DiagnosticReportMapper.isFilingComment(observation);
    }

    private String prepareCodeElement(Observation observation) {
        if (observation.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(observation.getCode());
        }

        throw new EhrMapperException("%s must contain a code element.".formatted(observation.getId()));
    }

    private CompoundStatementClassCode prepareClassCode(List<MultiStatementObservationHolder> derivedObservations) {
        return derivedObservations.stream()
            .map(MultiStatementObservationHolder::getObservation)
            .filter(this::observationHasNonCommentNoteCode)
            .map($ -> CompoundStatementClassCode.BATTERY)
            .findFirst()
            .orElse(CompoundStatementClassCode.CLUSTER);
    }

    private Optional<String> prepareInterpretation(Observation observation) {
        if (observation.hasInterpretation()) {
            return observation.getInterpretation().getCoding().stream()
                .filter(this::isInterpretationCode)
                .findFirst()
                .map(structuredObservationValueMapper::mapInterpretation);
        }

        return Optional.empty();
    }

    private boolean isInterpretationCode(Coding coding) {
        String codingSystem = coding.getSystem();
        String code = coding.getCode();

        return (coding.hasSystem() && codingSystem.equals(INTERPRETATION_CODE_SYSTEM))
            && INTERPRETATION_CODES.contains(code);
    }

    private Optional<String> extractUnit(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.hasHigh() && referenceRange.getHigh().hasUnit()) {
            return Optional.of(referenceRange.getHigh().getUnit());
        } else if (referenceRange.hasLow() && referenceRange.getLow().hasUnit()) {
            return Optional.of(referenceRange.getLow().getUnit());
        }

        return Optional.empty();
    }

    private boolean isRangeUnitValid(String unit, Quantity quantity) {
        return quantity.hasUnit() && !unit.equals(quantity.getUnit());
    }

    private boolean isInterpretationCodeMapped(String observationStatementAsText) {
        return observationStatementAsText.contains(INTERPRETATION_CODE);
    }

    private boolean hasValidType(Observation.ObservationRelatedComponent observationRelatedComponent) {
        return observationRelatedComponent.hasType()
            && HAS_MEMBER_TYPE.equals(observationRelatedComponent.getType().name());
    }

    private boolean hasValidComment(Observation observation) {
        return observation.hasComment() && DiagnosticReportMapper.isFilingComment(observation);
    }

    private MultiStatementObservationHolder createHolder(Observation observationAssociatedWithSpecimen) {
        return new MultiStatementObservationHolder(observationAssociatedWithSpecimen, messageContext, randomIdGeneratorService);
    }
}
