package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.EffectiveTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationStatementMapper {
    private static final Mustache OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_observation_statement_effective_time_template.mustache");
    private final MessageContext messageContext;

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        var observationStatementTemplateParameters = ObservationStatementTemplateParameters.builder()
            .observationStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
            .comment(observation.getComment())
            .issued(DateFormatUtil.formatDate(observation.getIssued()))
            .isNested(isNested)
            .effectiveTime(EffectiveTimeMappingUtils.prepareEffectiveTimeForObservation(observation))
            .build();

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE, observationStatementTemplateParameters);
    }
}
