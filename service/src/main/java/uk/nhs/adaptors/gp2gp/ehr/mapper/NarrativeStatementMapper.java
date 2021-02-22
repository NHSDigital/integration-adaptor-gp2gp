package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDateTimeTypeComputerReadable;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatInstantTypeComputerReadable;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NarrativeStatementMapper {

    private final MessageContext messageContext;

    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");

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
            return formatDateTimeTypeComputerReadable(observation.getEffectiveDateTimeType());
        } else if (observation.hasEffectivePeriod()) {
            return formatDateTimeTypeComputerReadable(observation.getEffectivePeriod().getStartElement());
        } else if (observation.hasIssuedElement()) {
            return formatInstantTypeComputerReadable(observation.getIssuedElement());
        } else {
            throw new EhrMapperException("Could not map effective date");
        }
    }
}