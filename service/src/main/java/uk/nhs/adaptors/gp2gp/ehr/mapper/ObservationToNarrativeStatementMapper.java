package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ObservationToNarrativeStatementMapper {

    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");

    private final MessageContext messageContext;
    private final ParticipantMapper participantMapper;

    public String mapObservationToNarrativeStatement(Observation observation, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getId()))
            .availabilityTime(getAvailabilityTime(observation))
            .comment(observation.getComment())
            .isNested(isNested);

        if (observation.hasPerformer()) {
            final String participantReference = idMapper.get(observation.getPerformerFirstRep());
            final String participantBlock = participantMapper
                .mapToParticipant(participantReference, ParticipantType.PERFORMER);
            narrativeStatementTemplateParameters.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters.build());
    }

    private String getAvailabilityTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return DateFormatUtil.toHl7Format(observation.getEffectiveDateTimeType());
        } else if (observation.hasEffectivePeriod()) {
            return DateFormatUtil.toHl7Format(observation.getEffectivePeriod().getStartElement());
        } else if (observation.hasIssuedElement()) {
            return toHl7Format(observation.getIssuedElement());
        } else {
            throw new EhrMapperException("Could not map effective date");
        }
    }
}
