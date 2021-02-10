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
    private static final String EFFECTIVE_TIME_XML = "<effectiveTime><center value=\"%s\"/></effectiveTime>";
    private static final String EFFECTIVE_PERIOD_XML = "<effectiveTime><low value=\"%s\"/><high value=\"%s\"/></effectiveTime>";
    private static final String EFFECTIVE_TIME_UNK_XML =  "<effectiveTime><center value=\"UNK\"/></effectiveTime>";

    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        var observationStatementTemplateParameters = ObservationStatementTemplateParameters.builder()
            .observationStatementId(randomIdGeneratorService.createNewId())
            .comment(observation.getComment())
            .issued(DateFormatUtil.formatDate(observation.getIssued()))
            .isNested(isNested)
            .effectiveTime(effectiveTime(observation))
            .build();

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE, observationStatementTemplateParameters);
    }

    private String effectiveTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return String.format(EFFECTIVE_TIME_XML, DateFormatUtil.formatDate(observation.getEffectiveDateTimeType().getValue()));
        } else if (observation.hasEffectivePeriod()) {
            return String.format(EFFECTIVE_PERIOD_XML, DateFormatUtil.formatDate(observation.getEffectivePeriod().getStart()),
                DateFormatUtil.formatDate(observation.getEffectivePeriod().getEnd()));
        } else {
            return EFFECTIVE_TIME_UNK_XML;
        }
    }
}
