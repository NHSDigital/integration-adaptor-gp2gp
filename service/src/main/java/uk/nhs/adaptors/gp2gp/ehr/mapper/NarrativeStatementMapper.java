package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.hl7.fhir.dstu3.model.Observation;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class NarrativeStatementMapper {

    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();

    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapObservationToNarrativeStatement(Observation observation, boolean isNested) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(randomIdGeneratorService.createNewId())
            .availabilityTime(getAvailabilityTime(observation))
            .comment(observation.getComment())
            .build();

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters);
    }

    private String getAvailabilityTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return DATE_TIME_FORMATTER.format(
                observation.getEffectiveDateTimeType().getValue()
                    .toInstant()
                    .atZone(ZoneId.of("Europe/London"))
                    .toLocalDateTime());
        } else if (observation.hasEffectivePeriod()) {
            return DATE_TIME_FORMATTER.format(
                observation.getEffectivePeriod().getStart()
                    .toInstant()
                    .atZone(ZoneId.of("Europe/London"))
                    .toLocalDateTime());
        } else {
            return DATE_TIME_FORMATTER.format(
                observation.getIssued()
                    .toInstant()
                    .atZone(ZoneId.of("Europe/London"))
                    .toLocalDateTime());
        }
    }
}
