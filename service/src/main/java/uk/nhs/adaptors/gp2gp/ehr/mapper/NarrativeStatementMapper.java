package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.hl7.fhir.dstu3.model.Observation;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class NarrativeStatementMapper {
    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");

    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapObservationToNarrativeStatement(Observation observation, boolean isNested) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(randomIdGeneratorService.createNewId())
            .availabilityTime(getAvailabilityTime(observation))
            .comment(observation.getComment())
            .isNested(isNested)
            .build();

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters);
    }

    private String getAvailabilityTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return DateFormatUtil.formatDate(observation.getEffectiveDateTimeType().getValue());
        } else if (observation.hasEffectivePeriod()) {
            return DateFormatUtil.formatDate((observation.getEffectivePeriod().getStart()));
        } else {
            return DateFormatUtil.formatDate((observation.getIssued()));
        }
    }
}
