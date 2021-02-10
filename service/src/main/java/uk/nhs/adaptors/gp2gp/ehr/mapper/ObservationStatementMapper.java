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

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        var observationStatementTemplateParameters = ObservationStatementTemplateParameters.builder()
            .observationStatementId(randomIdGeneratorService.createNewId())
            .comment(observation.getComment())
            .issued(DateFormatUtil.formatDate(observation.getIssued()))
            .isNested(isNested)
            .build();

        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            observationStatementTemplateParameters.setEffectiveTime(
                DateFormatUtil.formatDate(observation.getEffectiveDateTimeType().getValue()));
            return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE, observationStatementTemplateParameters);
        } else if (observation.hasEffectivePeriod()) {
            observationStatementTemplateParameters.setEffectiveTimeLow(
                DateFormatUtil.formatDate(observation.getEffectivePeriod().getStart()));
            observationStatementTemplateParameters.setEffectiveTimeHigh(
                DateFormatUtil.formatDate(observation.getEffectivePeriod().getEnd()));
            return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_PERIOD_TEMPLATE, observationStatementTemplateParameters);
        } else {
            observationStatementTemplateParameters.setEffectiveTime("UNK");
            return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE, observationStatementTemplateParameters);
        }
    }
}
