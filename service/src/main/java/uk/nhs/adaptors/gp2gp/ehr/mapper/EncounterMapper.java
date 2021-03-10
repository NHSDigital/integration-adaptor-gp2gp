package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EncounterMapper {
    private static final Mustache ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_encounter_to_ehr_composition_template.mustache");
    private static final String COMPLETE_CODE = "COMPLETE";

    private final MessageContext messageContext;
    private final EncounterComponentsMapper encounterComponentsMapper;

    // TODO: Refactor to use resource file for vocabulary codes
    private static final String[] EHR_COMPOSITION_VOCABULARY_CODES = {
        "24561000000109", "37361000000105", "37351000000107", "37341000000109", "37331000000100", "15611000000104", "37321000000102",
        "24571000000102", "25561000000105", "24581000000100", "25571000000103", "24591000000103", "25581000000101", "37311000000108",
        "37301000000106", "37281000000105", "37291000000107", "24601000000109", "25591000000104", "25601000000105", "24611000000106",
        "25611000000107", "24621000000100", "25631000000104", "25621000000101", "25641000000108", "24631000000103", "25651000000106",
        "24641000000107", "25661000000109", "24651000000105", "24661000000108", "25671000000102", "25681000000100", "25691000000103",
        "25701000000103", "24671000000101", "25711000000101", "24681000000104", "24691000000102", "24701000000102", "25741000000100",
        "25731000000109", "37271000000108", "24711000000100", "24721000000106", "24881000000103", "25751000000102", "25761000000104",
        "24731000000108", "25771000000106", "25791000000105", "25781000000108", "24741000000104", "25801000000109", "109341000000100",
        "24751000000101", "25811000000106"
    };

    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String EHR_COMPOSITION_SNOMED_CODE = "<code code=\"%s\" displayName=\"%s\" codeSystem"
        + "=\"2.16.840.1.113883.2.1.3.2.4.15\">%s</code>";
    private static final String EHR_COMPOSITION_OTHER_REPORT_CODE = "<code code=\"24591000000103\" displayName=\"Other report\" "
        + "codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\">%s</code>";
    private static final String EHR_COMPOSITION_CODE_TEXT = "<originalText>%s</originalText>";

    public String mapEncounterToEhrComposition(Encounter encounter) {
        String components = encounterComponentsMapper.mapComponents(encounter);

        var encounterStatementTemplateParameters = EncounterTemplateParameters.builder()
            .encounterStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Encounter, encounter.getId()))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForEncounter(encounter))
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForEncounter(encounter))
            .status(COMPLETE_CODE)
            .components(components)
            .code(buildCode(encounter));

        return TemplateUtils.fillTemplate(ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
            encounterStatementTemplateParameters.build());
    }

    private String buildCode(Encounter encounter) {
        var type = encounter.getType()
            .stream()
            .findFirst()
            .orElseThrow(() -> new EhrMapperException("Could not map Encounter type"));

        var coding = type.getCoding()
            .stream()
            .findFirst()
            .orElseThrow(() -> new EhrMapperException("Could not map Encounter type coding"));

        if (isSnomedAndWithinEhrCompositionVocabularyCodes(coding)) {
            return String.format(EHR_COMPOSITION_SNOMED_CODE, coding.getCode(), coding.getDisplay(), buildCodeText(type));
        } else {
            return String.format(EHR_COMPOSITION_OTHER_REPORT_CODE, buildCodeText(type));
        }
    }

    private boolean isSnomedAndWithinEhrCompositionVocabularyCodes(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM)
            && Arrays.asList(EHR_COMPOSITION_VOCABULARY_CODES).contains(coding.getCode());
    }

    private String buildCodeText(CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return String.format(EHR_COMPOSITION_CODE_TEXT, codeableConcept.getText());
        } else {
            return StringUtils.EMPTY;
        }
    }
}
