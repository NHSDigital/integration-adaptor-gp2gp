package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EncounterStatementMapper {
    private static final Mustache ENCOUNTER_STATEMENT_TO_EHR_COMPOSTION_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_encounter_statement_to_ehr_composition_template.mustache");
    private static final String COMPLETE_CODE = "COMPLETE";
    private static final String DEFAULT_TIME_VALUE = "<center nullFlavour=\"UNK\"/>";
    private static final String EFFECTIVE_LOW_TEMPLATE = "<low value=\"%s\"/>";
    private static final String EFFECTIVE_HIGH_TEMPLATE = "<high value=\"%s\"/>";

    private final MessageContext messageContext;

    public String mapEncounterToEncounterStatement(Encounter encounter) {
        var encounterStatementTemplateParameters = EncounterStatementTemplateParameters.builder()
            .encounterStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Encounter, encounter.getId()))
            .status(COMPLETE_CODE);

        Period period = encounter.getPeriod();

        if (period == null || !isPeriodPopulated(period)) {
            encounterStatementTemplateParameters.effectiveTime(DEFAULT_TIME_VALUE);
        } else {
            encounterStatementTemplateParameters.effectiveTime(prepareProperEffectiveTime(period));
        }

        return TemplateUtils.fillTemplate(ENCOUNTER_STATEMENT_TO_EHR_COMPOSTION_TEMPLATE,
            encounterStatementTemplateParameters.build());
    }

    private boolean isPeriodPopulated(Period period) {
        return period.getStart() != null;
    }

    private String prepareProperEffectiveTime(Period period) {
        String effectiveTime = String.format(EFFECTIVE_LOW_TEMPLATE, DateFormatUtil.formatDate(period.getStart()));
        if (period.getEnd() != null) {
            effectiveTime += System.lineSeparator() + String.format(EFFECTIVE_HIGH_TEMPLATE, DateFormatUtil.formatDate(period.getEnd()));
        }

        return effectiveTime;
    }
}
