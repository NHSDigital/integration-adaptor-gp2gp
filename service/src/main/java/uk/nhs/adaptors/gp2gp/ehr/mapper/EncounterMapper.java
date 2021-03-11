package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String EHR_COMPOSITION_SNOMED_CODE = "<code code=\"%s\" displayName=\"%s\" codeSystem"
        + "=\"2.16.840.1.113883.2.1.3.2.4.15\">%s</code>";
    private static final String EHR_COMPOSITION_OTHER_REPORT_CODE = "<code code=\"24591000000103\" displayName=\"Other report\" "
        + "codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\">%s</code>";
    private static final String EHR_COMPOSITION_CODE_TEXT = "<originalText>%s</originalText>";
    private static final String[] EHR_COMPOSITION_NAME_VOCABULARY_CODES = getEhrCompositionNameVocabularyCodes();

    private final MessageContext messageContext;
    private final EncounterComponentsMapper encounterComponentsMapper;

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
            .filter(this::isSnomedAndWithinEhrCompositionVocabularyCodes)
            .findFirst();

        if (coding.isPresent()) {
            return String.format(EHR_COMPOSITION_SNOMED_CODE, coding.get().getCode(), coding.get().getDisplay(), buildCodeText(type));
        } else {
            return String.format(EHR_COMPOSITION_OTHER_REPORT_CODE, buildCodeText(type));
        }
    }

    private boolean isSnomedAndWithinEhrCompositionVocabularyCodes(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM)
            && Arrays.asList(EHR_COMPOSITION_NAME_VOCABULARY_CODES).contains(coding.getCode());
    }

    private String buildCodeText(CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return String.format(EHR_COMPOSITION_CODE_TEXT, codeableConcept.getText());
        } else {
            return StringUtils.EMPTY;
        }
    }

    @SneakyThrows
    private static String[] getEhrCompositionNameVocabularyCodes() {
        InputStream inputStream = EncounterMapper.class.getClassLoader().getResourceAsStream("ehr_composition_name_vocabulary_codes.txt");
        String ehrCompositionNameCodes = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        return  ehrCompositionNameCodes.split("\n");
    }
}
