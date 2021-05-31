package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper.DUMMY_OBSERVATION_ID_PREFIX;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationRelatedComponent;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CompoundStatementClassCode;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.ObservationCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.ObservationStatementTemplateParameters;
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

    private static final String DATA_ABSENT_PREFIX = "Data Absent: ";
    private static final String INTERPRETATION_PREFIX = "Interpretation: ";
    private static final String BODY_SITE_PREFIX = "Site: ";
    private static final String METHOD_PREFIX = "Method: ";
    private static final String RANGE_UNITS_PREFIX = "Range Units: ";

    private static final String COMMENT_NOTE_CODE = "37331000000100";

    private static final List<Class<? extends Type>> UNHANDLED_TYPES = List.of(SampledData.class, Attachment.class);

    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";
    private static final Set<String> INTERPRETATION_CODES = Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA");

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapObservationToCompoundStatement(Observation observationAssociatedWithSpecimen) {
        return new ObservationMapper.InnerMapper(observationAssociatedWithSpecimen).map();
    }

    @RequiredArgsConstructor
    private class InnerMapper {
        private final Observation observationAssociatedWithSpecimen;

        private IdMapper idMapper;

        public String map() {
            idMapper = messageContext.getIdMapper();

            List<Observation> derivedObservations = observationAssociatedWithSpecimen.getRelated().stream()
                .filter(observationRelation -> observationRelation.getType() == Observation.ObservationRelationshipType.HASMEMBER)
                .map(ObservationRelatedComponent::getTarget)
                .map(Reference::getReferenceElement)
                .map(referenceElement -> messageContext.getInputBundleHolder().getResource(referenceElement))
                .flatMap(Optional::stream)
                .map(Observation.class::cast)
                .collect(Collectors.toList());

            var classCode = prepareClassCode(derivedObservations);
            var compoundStatementId = idMapper.getOrNew(ResourceType.Observation, observationAssociatedWithSpecimen.getIdElement());
            var codeElement = prepareCodeElement(observationAssociatedWithSpecimen);
            var effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observationAssociatedWithSpecimen);
            var availabilityTimeElement =
                StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observationAssociatedWithSpecimen);
            var narrativeStatements = prepareNarrativeStatements(observationAssociatedWithSpecimen);
            var statementsForDerivedObservations = prepareStatementsForDerivedObservations(derivedObservations);

            if (isEmpty(narrativeStatements) && isEmpty(statementsForDerivedObservations)) {
                narrativeStatements = prepareEmptyNarrativeStatement(observationAssociatedWithSpecimen);
            }

            var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
                .classCode(classCode.getCode())
                .compoundStatementId(compoundStatementId)
                .codeElement(codeElement)
                .effectiveTime(effectiveTime)
                .availabilityTimeElement(availabilityTimeElement)
                .narrativeStatements(narrativeStatements)
                .statementsForDerivedObservations(statementsForDerivedObservations);

            prepareParticipant(observationAssociatedWithSpecimen)
                .ifPresent(observationCompoundStatementTemplateParameters::participant);

            return TemplateUtils.fillTemplate(
                OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
                observationCompoundStatementTemplateParameters.build()
            );
        }

        private String prepareNarrativeStatements(Observation observation) {
            StringBuilder narrativeStatementsBlock = new StringBuilder();

            if (observation.hasComment()) {
                CommentType commentType = prepareCommentType(observation);

                narrativeStatementsBlock.append(
                    mapObservationToNarrativeStatement(observation, observation.getComment(), commentType.getCode(), true)
                );
            }

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getDataAbsentReason())
                .map(DATA_ABSENT_PREFIX::concat)
                .map(comment ->
                    mapObservationToNarrativeStatement(observation, comment, CommentType.LABORATORY_RESULT_DETAIL.getCode(), true)
                )
                .ifPresent(narrativeStatementsBlock::append);

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getInterpretation())
                .map(INTERPRETATION_PREFIX::concat)
                .map(interpretation ->
                    mapObservationToNarrativeStatement(
                        observation, interpretation, CommentType.LABORATORY_RESULT_DETAIL.getCode(), true
                    )
                )
                .ifPresent(narrativeStatementsBlock::append);

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getBodySite())
                .map(BODY_SITE_PREFIX::concat)
                .map(comment ->
                    mapObservationToNarrativeStatement(observation, comment, CommentType.LABORATORY_RESULT_DETAIL.getCode(), true)
                )
                .ifPresent(narrativeStatementsBlock::append);

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getMethod())
                .map(METHOD_PREFIX::concat)
                .map(comment ->
                    mapObservationToNarrativeStatement(observation, comment, CommentType.LABORATORY_RESULT_DETAIL.getCode(), true)
                )
                .ifPresent(narrativeStatementsBlock::append);

            if (observation.hasReferenceRange() && observation.hasValueQuantity()) {
                Observation.ObservationReferenceRangeComponent referenceRange = observation.getReferenceRangeFirstRep();

                extractUnit(referenceRange)
                    .filter(referenceRangeUnit -> isRangeUnitValid(referenceRangeUnit, observation.getValueQuantity()))
                    .map(RANGE_UNITS_PREFIX::concat)
                    .map(comment ->
                        mapObservationToNarrativeStatement(observation, comment, CommentType.COMPLEX_REFERENCE_RANGE.getCode(), true)
                    )
                    .ifPresent(narrativeStatementsBlock::append);
            }

            return narrativeStatementsBlock.toString();
        }

        private String prepareEmptyNarrativeStatement(Observation observation) {
            return mapObservationToNarrativeStatement(observation, "EMPTY REPORT",
                CommentType.AGGREGATE_COMMENT_SET.getCode(), false);
        }

        private String mapObservationToNarrativeStatement(Observation observation, String comment, String commentType,
            boolean mapParticipants) {
            var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
                .narrativeStatementId(randomIdGeneratorService.createNewId())
                .commentType(commentType)
                .commentDate(DateFormatUtil.toHl7Format(observation.getIssuedElement()))
                .comment(comment)
                .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observation));

            if (mapParticipants) {
                prepareParticipant(observation).ifPresent(narrativeStatementTemplateParameters::participant);
            }

            return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build());
        }

        private String prepareStatementsForDerivedObservations(List<Observation> derivedObservations) {
            StringBuilder derivedObservationsBlock = new StringBuilder();

            derivedObservations.forEach(derivedObservation -> {
                var observationStatement = prepareObservationStatement(derivedObservation);
                var narrativeStatements = prepareNarrativeStatements(derivedObservation);

                if (!observationStatement.isBlank() && !narrativeStatements.isBlank()) {
                    var compoundStatementId = idMapper.getOrNew(ResourceType.Observation, derivedObservation.getIdElement());
                    var codeElement = prepareCodeElement(derivedObservation);
                    var effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForObservation(derivedObservation);
                    var availabilityTimeElement = StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(derivedObservation);

                    var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
                        .classCode(CompoundStatementClassCode.CLUSTER.getCode())
                        .compoundStatementId(compoundStatementId)
                        .codeElement(codeElement)
                        .effectiveTime(effectiveTime)
                        .availabilityTimeElement(availabilityTimeElement)
                        .observationStatement(observationStatement)
                        .narrativeStatements(narrativeStatements);

                    prepareParticipant(derivedObservation).ifPresent(observationCompoundStatementTemplateParameters::participant);

                    derivedObservationsBlock.append(
                        TemplateUtils.fillTemplate(
                            OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
                            observationCompoundStatementTemplateParameters.build()
                        )
                    );
                } else {
                    derivedObservationsBlock.append(observationStatement).append(narrativeStatements);
                }
            });

            return derivedObservationsBlock.toString();
        }

        private String prepareObservationStatement(Observation observation) {
            if (observationHasNonCommentNoteCode(observation)) {
                return mapObservationToObservationStatement(observation);
            }

            return StringUtils.EMPTY;
        }

        private String mapObservationToObservationStatement(Observation observation) {
            var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
                .observationStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getIdElement()))
                .codeElement(prepareCodeElement(observation))
                .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation))
                .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observation));

            if (observation.hasValue()) {
                Type value = observation.getValue();

                if (UNHANDLED_TYPES.contains(value.getClass())) {
                    LOGGER.info(
                        "Observation value type {} not supported. Mapping for this field is skipped",
                        observation.getValue().getClass()
                    );
                } else if (structuredObservationValueMapper.isStructuredValueType(value)) {
                    observationStatementTemplateParametersBuilder.value(
                        structuredObservationValueMapper.mapObservationValueToStructuredElement(value)
                    );
                }
            }

            if (observation.hasReferenceRange() && observation.hasValueQuantity()) {
                observationStatementTemplateParametersBuilder.referenceRange(
                    structuredObservationValueMapper.mapReferenceRangeType(observation.getReferenceRangeFirstRep()));
            }

            prepareInterpretation(observation).ifPresent(observationStatementTemplateParametersBuilder::interpretation);
            prepareParticipant(observation).ifPresent(observationStatementTemplateParametersBuilder::participant);

            return TemplateUtils.fillTemplate(
                OBSERVATION_STATEMENT_TEMPLATE,
                observationStatementTemplateParametersBuilder.build()
            );
        }

        private boolean observationHasNonCommentNoteCode(Observation observation) {
            return observation.hasCode() && !CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(COMMENT_NOTE_CODE));
        }

        private String prepareCodeElement(Observation observation) {
            if (observation.hasCode()) {
                return codeableConceptCdMapper.mapCodeableConceptToCd(observation.getCode());
            }

            return StringUtils.EMPTY;
        }

        private CompoundStatementClassCode prepareClassCode(List<Observation> derivedObservations) {
            return derivedObservations.stream()
                .filter(this::observationHasNonCommentNoteCode)
                .map($ -> CompoundStatementClassCode.BATTERY)
                .findFirst()
                .orElse(CompoundStatementClassCode.CLUSTER);
        }

        private CommentType prepareCommentType(Observation observation) {
            if (observation.getIdElement().getIdPart().contains(DUMMY_OBSERVATION_ID_PREFIX)) {
                return CommentType.AGGREGATE_COMMENT_SET;
            }

            if (observation.hasSpecimen()) {
                return CommentType.LABORATORY_RESULT_COMMENT;
            }

            return CommentType.USER_COMMENT;
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

        private Optional<String> prepareParticipant(Observation observation) {
            if (observation.hasPerformer()) {
                final String participantReference = idMapper.get(observation.getPerformerFirstRep());

                return Optional.ofNullable(
                    participantMapper.mapToParticipant(participantReference, ParticipantType.PERFORMER)
                );
            }

            return Optional.empty();
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
    }
}
