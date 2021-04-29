package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.ObservationCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.ObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ObservationMapper {

    private static final Mustache OBSERVATION_NARRATIVE_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_narrative_statement_template.mustache");
    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_statement_template.mustache");
    private static final Mustache OBSERVATION_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_compound_statement_template.mustache");

    private static final List<Class<? extends Type>> UNHANDLED_TYPES = ImmutableList.of(SampledData.class, Attachment.class);

    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";
    private static final Set<String> INTERPRETATION_CODES = Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA");

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapObservationToCompoundStatement(Observation observationAssociatedWithSpecimen, List<Observation> observations) {
        List<Observation> derivedObservations = observations.stream()
            .filter(observation ->
                observation.getRelated().stream().anyMatch(
                    observationRelation -> observationRelation.getType() == Observation.ObservationRelationshipType.DERIVEDFROM
                        && observationRelation.getTarget().getReference().equals(observationAssociatedWithSpecimen.getId())
                )
            )
            .collect(Collectors.toList());

        final IdMapper idMapper = messageContext.getIdMapper();

        var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
            .compoundStatementId(
                idMapper.getOrNew(ResourceType.Observation, observationAssociatedWithSpecimen.getId())
            )
            .codeElement(prepareCodeElement(observationAssociatedWithSpecimen))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observationAssociatedWithSpecimen))
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observationAssociatedWithSpecimen));

        String observationStatement = mapObservationToObservationStatement(idMapper, observationAssociatedWithSpecimen);

        StringBuilder narrativeStatementsBlock = new StringBuilder();

        if (observationAssociatedWithSpecimen.hasComment()) {
            narrativeStatementsBlock.append(
                mapObservationToNarrativeStatement(idMapper, observationAssociatedWithSpecimen)
            );
        }

        derivedObservations.forEach(derivedObservation -> {
            if (derivedObservation.hasComment()) {
                narrativeStatementsBlock.append(
                    mapObservationToNarrativeStatement(idMapper, derivedObservation)
                );
            }
        });

        observationCompoundStatementTemplateParameters
            .observationStatement(observationStatement)
            .narrativeStatements(narrativeStatementsBlock.toString());

        return TemplateUtils.fillTemplate(
            OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
            observationCompoundStatementTemplateParameters.build()
        );
    }

    private String mapObservationToNarrativeStatement(IdMapper idMapper, Observation observation) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getId()))
            .commentType(prepareCommentType(observation).getCode())
            .issuedDate(DateFormatUtil.toHl7Format(observation.getIssued().toInstant()))
            .comment(observation.getComment())
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observation));

        if (observation.hasPerformer()) {
            final String participantReference = idMapper.get(observation.getPerformerFirstRep());
            final String participantBlock = participantMapper
                .mapToParticipant(participantReference, ParticipantType.PERFORMER);
            narrativeStatementTemplateParameters.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build());
    }

    private String mapObservationToObservationStatement(IdMapper idMapper, Observation observation) {
        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getId()))
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

        if (observation.hasInterpretation()) {
            observation.getInterpretation().getCoding().stream()
                .filter(this::isInterpretationCode)
                .findFirst()
                .ifPresent(coding ->
                    observationStatementTemplateParametersBuilder.interpretation(
                        structuredObservationValueMapper.mapInterpretation(coding))
            );
        }

        if (observation.hasPerformer()) {
            final String participantReference = idMapper.get(observation.getPerformerFirstRep());
            final String participantBlock = participantMapper
                .mapToParticipant(participantReference, ParticipantType.PERFORMER);

            observationStatementTemplateParametersBuilder.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE,
            observationStatementTemplateParametersBuilder.build());
    }

    private String prepareCodeElement(Observation observation) {
        if (observation.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(observation.getCode());
        }
        throw new EhrMapperException("Observation code is not present");
    }

    private CommentType prepareCommentType(Observation observation) {
        if (observation.hasSpecimen()) {
            return CommentType.LABORATORY_RESULT_COMMENT;
        }

        return CommentType.USER_COMMENT;
    }

    private boolean isInterpretationCode(Coding coding) {
        String codingSystem = coding.getSystem();
        String code = coding.getCode();

        return (coding.hasSystem() && codingSystem.equals(INTERPRETATION_CODE_SYSTEM))
            && INTERPRETATION_CODES.contains(code);
    }
}
