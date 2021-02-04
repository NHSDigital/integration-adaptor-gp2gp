package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

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
        .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
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
        if (observation.hasEffectiveDateTimeType()) {
            return DATE_TIME_FORMATTER.format(observation.getEffectiveDateTimeType().getValue().toInstant().atOffset(ZoneOffset.UTC));
        } else if (observation.hasEffectivePeriod()) {
            return DATE_TIME_FORMATTER.format(observation.getEffectivePeriod().getStart().toInstant().atOffset(ZoneOffset.UTC));
        } else {
            return DATE_TIME_FORMATTER.format(observation.getIssued().toInstant().atOffset(ZoneOffset.UTC));
        }
    }
}
