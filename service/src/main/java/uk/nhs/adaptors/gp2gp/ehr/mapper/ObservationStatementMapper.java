package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;
import org.hl7.fhir.dstu3.model.Observation;

import com.github.mustachejava.Mustache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationStatementMapper {

    private static final Mustache OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_observation_statement_effective_time_template.mustache");
    private static final Mustache OBSERVATION_STATEMENT_EFFECTIVE_PERIOD_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_observation_statement_effective_period_template.mustache");

    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapObservationToNarrativeStatement(Observation observation, boolean isNested) {
        var observationStatementTemplateParameters = ObservationStatementTemplateParameters.builder()
            .observationStatementId(randomIdGeneratorService.createNewId())
            .comment(observation.getComment())
            .issued(DateFormatUtil.formatDate(observation.getIssued()))
            .isNested(isNested)
            .effectiveTime(getEffectiveTime(observation))
            .effectiveTimeLow(DateFormatUtil.formatDate(observation.getEffectivePeriod().getStart()))
            .effectiveTimeHigh(DateFormatUtil.formatDate(observation.getEffectivePeriod().getEnd()))
            .build();

        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE, observationStatementTemplateParameters);
        } else if (observation.hasEffectivePeriod()) {
            return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_PERIOD_TEMPLATE, observationStatementTemplateParameters);
        } else {
            return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE, observationStatementTemplateParameters);
        }
    }

    private String getEffectiveTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return DateFormatUtil.formatDate(observation.getEffectiveDateTimeType().getValue());
        } else {
            return "UNK";
        }
    }
}
