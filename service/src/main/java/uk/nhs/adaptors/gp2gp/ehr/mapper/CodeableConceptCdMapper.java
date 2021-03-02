package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.Extension;

import com.github.mustachejava.Mustache;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.CodeableConceptCdTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Slf4j
public class CodeableConceptCdMapper {

    private static final Mustache CODEABLE_CONCEPT_CD_TEMPLATE = TemplateUtils
        .loadTemplate("codeable_concept_cd_template.mustache");
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String SNOMED_SYSTEM_CODE = "2.16.840.1.113883.2.1.3.2.4.15";
    private static final String DESCRIPTION_DISPLAY = "descriptionDisplay";
    private static final String DESCRIPTION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";

    public String mapCodeableConceptToCd(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = findMainCode(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());
        var originalText = findOriginalText(codeableConcept, mainCode);
        originalText.ifPresent(builder::mainOriginalText);

        if (mainCode.isPresent()) {
            builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
            var code = extractCode(mainCode.get());
            var displayText = findDisplayText(mainCode.get());

            code.ifPresent(builder::mainCode);
            displayText.ifPresent(builder::mainDisplayName);
        }

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    private Optional<Coding> findMainCode(CodeableConcept codeableConcept) {
        return codeableConcept.getCoding()
            .stream()
            .filter(this::isSnomed)
            .findFirst();
    }

    private Optional<String> extractCode(Coding coding) {
        return Optional.of(coding.getCode());
    }

    private Optional<String> findOriginalText(CodeableConcept codeableConcept, Optional<Coding> coding) {
        if (coding.isPresent()) {
            if (codeableConcept.hasText()) {
                return Optional.of(codeableConcept.getText());
            } else {
                var extension = retrieveDisplayExtension(coding.get());
                return extension.map(value -> value
                    .getExtension()
                    .stream()
                    .filter(extension1 -> extension1.getUrl().equals(DESCRIPTION_DISPLAY))
                    .map(extension1 -> extension1.getValue().toString())
                    .findFirst()).orElseGet(() -> Optional.of(coding.get().getDisplay()));
            }
        } else {
            return CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
        }
    }

    private Optional<String> findDisplayText(Coding coding) {
        return Optional.of(coding.getDisplay());
    }

    private Optional<Extension> retrieveDisplayExtension(Coding coding) {
        return coding
            .getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(DESCRIPTION_URL))
            .findFirst();
    }

    private boolean isSnomed(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM);
    }
}
