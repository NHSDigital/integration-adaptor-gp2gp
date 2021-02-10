package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NarrativeStatementMapper {

    private final MessageContext messageContext;

    private static final String UK_ZONE_ID = "Europe/London";
    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();

    public String mapObservationToNarrativeStatement(Observation observation, boolean isNested) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
            .availabilityTime(getAvailabilityTime(observation))
            .comment(observation.getComment())
            .isNested(isNested)
            .build();

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters);
    }

    private String getAvailabilityTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return formatDate(observation.getEffectiveDateTimeType().getValue());
        } else if (observation.hasEffectivePeriod()) {
            return formatDate(observation.getEffectivePeriod().getStart());
        } else if (observation.hasIssued()) {
            return formatDate(observation.getIssued());
        } else {
            throw new EhrMapperException("Could not map effective date");
        }
    }

    private String formatDate(Date date) {
        return DATE_TIME_FORMATTER.format(
            date.toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }
}
