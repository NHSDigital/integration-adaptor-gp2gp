package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnostic_report;

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
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report.ObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ObservationMapper {

    private static final Mustache OBSERVATION_NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("observation_narrative_statement_template.mustache");
    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_statement_template.mustache");

    private static final List<Class<? extends Type>> UNHANDLED_TYPES = ImmutableList.of(SampledData.class, Attachment.class);

    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";
    private static final Set<String> INTERPRETATION_CODES = Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA");

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapObservationToNarrativeStatement(Observation observation) {
        final IdMapper idMapper = messageContext.getIdMapper();

        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getId()))
            .commentType("comment type here") // TODO: LABORATORY RESULT COMMENT(E141) or USER COMMENT here
            .issuedDate("issued date here")
            .comment(observation.getComment());

        if (observation.hasPerformer()) {
            final String participantReference = idMapper.get(observation.getPerformerFirstRep());
            final String participantBlock = participantMapper
                .mapToParticipant(participantReference, ParticipantType.PERFORMER);
            narrativeStatementTemplateParameters.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build());
    }

    public String mapObservationToObservationStatement(Observation observation) {
        final IdMapper idMapper = messageContext.getIdMapper();

        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getId()))
            .code(prepareCode(observation))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation))
            .issuedDate(DateFormatUtil.toHl7Format(observation.getIssuedElement()));

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

    private String prepareCode(Observation observation) {
        if (observation.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(observation.getCode());
        }
        throw new EhrMapperException("Observation code is not present");
    }

    private boolean isInterpretationCode(Coding coding) {
        String codingSystem = coding.getSystem();
        String code = coding.getCode();

        return (coding.hasSystem() && codingSystem.equals(INTERPRETATION_CODE_SYSTEM))
            && INTERPRETATION_CODES.contains(code);
    }
}
