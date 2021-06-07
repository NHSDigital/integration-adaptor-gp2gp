package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

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

            CompoundStatementClassCode classCode = prepareClassCode(derivedObservations);
            String observationStatement = prepareObservationStatement(observationAssociatedWithSpecimen, classCode)
                .orElse(StringUtils.EMPTY);
            String narrativeStatements = prepareNarrativeStatements(observationAssociatedWithSpecimen)
                .orElse(StringUtils.EMPTY);

            if (derivedObservations.isEmpty()) {
                return observationStatement + narrativeStatements;
            }

            String compoundStatementId = idMapper.getOrNew(ResourceType.Observation, observationAssociatedWithSpecimen.getIdElement());
            String codeElement = prepareCodeElement(observationAssociatedWithSpecimen);
            String effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observationAssociatedWithSpecimen);
            String availabilityTimeElement =
                StatementTimeMappingUtils.prepareAvailabilityTime(observationAssociatedWithSpecimen.getIssuedElement());
            String statementsForDerivedObservations = prepareStatementsForDerivedObservations(derivedObservations);

            var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
                .classCode(classCode.getCode())
                .compoundStatementId(compoundStatementId)
                .codeElement(codeElement)
                .effectiveTime(effectiveTime)
                .availabilityTimeElement(availabilityTimeElement)
                .observationStatement(observationStatement)
                .narrativeStatements(narrativeStatements)
                .statementsForDerivedObservations(statementsForDerivedObservations);

            prepareParticipant(observationAssociatedWithSpecimen)
                .ifPresent(observationCompoundStatementTemplateParameters::participant);

            return TemplateUtils.fillTemplate(
                OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
                observationCompoundStatementTemplateParameters.build()
            );
        }

        private Optional<String> prepareObservationStatement(Observation observation, CompoundStatementClassCode classCode) {
            if (observationHasNonCommentNoteCode(observation) && classCode.equals(CompoundStatementClassCode.CLUSTER)) {
                return Optional.of(mapObservationToObservationStatement(observation));
            }

            return Optional.empty();
        }

        private String mapObservationToObservationStatement(Observation observation) {
            var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
                .observationStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getIdElement()))
                .codeElement(prepareCodeElement(observation))
                .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation))
                .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTime(observation.getIssuedElement()));

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

        private Optional<String> prepareNarrativeStatements(Observation observation) {
            StringBuilder narrativeStatementsBlock = new StringBuilder();

            if (observation.hasComment()) {
                CommentType commentType = prepareCommentType(observation);

                narrativeStatementsBlock.append(
                    mapObservationToNarrativeStatement(observation, observation.getComment(), commentType.getCode())
                );
            }

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getDataAbsentReason())
                .map(DATA_ABSENT_PREFIX::concat)
                .map(comment ->
                    mapObservationToNarrativeStatement(observation, comment, CommentType.LABORATORY_RESULT_DETAIL.getCode())
                )
                .ifPresent(narrativeStatementsBlock::append);

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getInterpretation())
                .map(INTERPRETATION_PREFIX::concat)
                .map(interpretation ->
                    mapObservationToNarrativeStatement(
                        observation, interpretation, CommentType.LABORATORY_RESULT_DETAIL.getCode()
                    )
                )
                .ifPresent(narrativeStatementsBlock::append);

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getBodySite())
                .map(BODY_SITE_PREFIX::concat)
                .map(comment ->
                    mapObservationToNarrativeStatement(observation, comment, CommentType.LABORATORY_RESULT_DETAIL.getCode())
                )
                .ifPresent(narrativeStatementsBlock::append);

            CodeableConceptMappingUtils.extractTextOrCoding(observation.getMethod())
                .map(METHOD_PREFIX::concat)
                .map(comment ->
                    mapObservationToNarrativeStatement(observation, comment, CommentType.LABORATORY_RESULT_DETAIL.getCode())
                )
                .ifPresent(narrativeStatementsBlock::append);

            if (observation.hasReferenceRange() && observation.hasValueQuantity()) {
                Observation.ObservationReferenceRangeComponent referenceRange = observation.getReferenceRangeFirstRep();

                extractUnit(referenceRange)
                    .filter(referenceRangeUnit -> isRangeUnitValid(referenceRangeUnit, observation.getValueQuantity()))
                    .map(RANGE_UNITS_PREFIX::concat)
                    .map(comment ->
                        mapObservationToNarrativeStatement(observation, comment, CommentType.COMPLEX_REFERENCE_RANGE.getCode())
                    )
                    .ifPresent(narrativeStatementsBlock::append);
            }

            if (!narrativeStatementsBlock.toString().isBlank()) {
                return Optional.of(narrativeStatementsBlock.toString());
            }

            return Optional.empty();
        }

        private String mapObservationToNarrativeStatement(Observation observation, String comment, String commentType) {
            var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
                .narrativeStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getIdElement()))
                .commentType(commentType)
                .commentDate(DateFormatUtil.toHl7Format(observation.getIssuedElement()))
                .comment(comment)
                .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTime(observation.getIssuedElement()));

            prepareParticipant(observation).ifPresent(narrativeStatementTemplateParameters::participant);

            return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build());
        }

        private String prepareStatementsForDerivedObservations(List<Observation> derivedObservations) {
            StringBuilder derivedObservationsBlock = new StringBuilder();

            derivedObservations.forEach(derivedObservation -> {
                Optional<String> observationStatement = prepareObservationStatement(derivedObservation, CompoundStatementClassCode.CLUSTER);
                Optional<String> narrativeStatements = prepareNarrativeStatements(derivedObservation);

                if (observationStatement.isPresent() && narrativeStatements.isPresent()) {
                    String compoundStatementId = idMapper.getOrNew(ResourceType.Observation, derivedObservation.getIdElement());
                    String codeElement = prepareCodeElement(derivedObservation);
                    String effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForObservation(derivedObservation);
                    String availabilityTimeElement =
                        StatementTimeMappingUtils.prepareAvailabilityTime(derivedObservation.getIssuedElement());

                    var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
                        .classCode(CompoundStatementClassCode.CLUSTER.getCode())
                        .compoundStatementId(compoundStatementId)
                        .codeElement(codeElement)
                        .effectiveTime(effectiveTime)
                        .availabilityTimeElement(availabilityTimeElement)
                        .observationStatement(observationStatement.get())
                        .narrativeStatements(narrativeStatements.get());

                    prepareParticipant(derivedObservation).ifPresent(observationCompoundStatementTemplateParameters::participant);

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
                final String participantReference = messageContext.getAgentDirectory().getAgentId(observation.getPerformerFirstRep());

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
