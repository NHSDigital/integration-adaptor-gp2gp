package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
    private static final String OTHER_REPORT_CODE = "24591000000103";
    private static final String OTHER_REPORT_DISPLAY = "Other report";
    private static final String NEW_LINE = "\n";
    private static final InputStream VOCAB_CODE_INPUT_STREAM =
        EncounterMapper.class.getClassLoader().getResourceAsStream("ehr_composition_name_vocabulary_codes.txt");
    private static final HashSet<String> EHR_COMPOSITION_NAME_VOCABULARY_CODES = getEhrCompositionNameVocabularyCodes();

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
            .code(buildCode(encounter))
            .displayName(buildDisplayName(encounter))
            .originalText(buildOriginalText(encounter));

        return TemplateUtils.fillTemplate(ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
            encounterStatementTemplateParameters.build());
    }

    private boolean isSnomedAndWithinEhrCompositionVocabularyCodes(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM)
            && EHR_COMPOSITION_NAME_VOCABULARY_CODES.contains(coding.getCode());
    }

    private Optional<Coding> extractCoding(Encounter encounter) {
        var type = encounter.getType()
            .stream()
            .findFirst()
            .orElseThrow(() -> new EhrMapperException("Could not map Encounter type"));

        return type.getCoding()
            .stream()
            .filter(this::isSnomedAndWithinEhrCompositionVocabularyCodes)
            .findFirst();
    }

    private String buildCode(Encounter encounter) {
        var coding = extractCoding(encounter);
        if (coding.isPresent()) {
            return coding.get().getCode();
        } else {
            return OTHER_REPORT_CODE;
        }
    }

    private String buildDisplayName(Encounter encounter) {
        var coding = extractCoding(encounter);
        if (coding.isPresent()) {
            return coding.get().getDisplay();
        } else {
            return OTHER_REPORT_DISPLAY;
        }
    }

    private String buildOriginalText(Encounter encounter) {
        if (encounter.getTypeFirstRep().hasText()) {
            return encounter.getTypeFirstRep().getText();
        } else {
            return StringUtils.EMPTY;
        }
    }

    @SneakyThrows
    private static HashSet<String> getEhrCompositionNameVocabularyCodes() {
        String ehrCompositionNameCodes = IOUtils.toString(VOCAB_CODE_INPUT_STREAM, StandardCharsets.UTF_8);

        return new HashSet<>(Arrays.asList(ehrCompositionNameCodes.split(NEW_LINE)));
    }
}
