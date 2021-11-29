package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.CodeableConceptCdTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CodeableConceptCdMapper {

    private static final Mustache CODEABLE_CONCEPT_CD_TEMPLATE = TemplateUtils
        .loadTemplate("codeable_concept_cd_template.mustache");
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String SNOMED_SYSTEM_CODE = "2.16.840.1.113883.2.1.3.2.4.15";
    private static final String DESCRIPTION_ID = "descriptionId";
    private static final String DESCRIPTION_DISPLAY = "descriptionDisplay";
    private static final String DESCRIPTION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";
    private static final String FIXED_ACTUAL_PROBLEM_CODE = "55607006";
    private static final String PROBLEM_DISPLAY_NAME = "Problem";

    public String mapCodeableConceptToCd(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = findMainCode(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());

        if (mainCode.isPresent()) {
            var extension = retrieveDescriptionExtension(mainCode.get());

            builder.mainCodeSystem(SNOMED_SYSTEM_CODE);

            Optional<String> code = extension.stream()
                .filter(displayExtension -> DESCRIPTION_ID.equals(displayExtension.getUrl()))
                .map(description -> description.getValue().toString())
                .findFirst()
                .or(() -> Optional.of(mainCode.get().getCode()));
            code.ifPresent(builder::mainCode);

            Optional<String> displayName = extension.stream()
                .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                .map(description -> description.getValue().toString())
                .findFirst()
                .or(() -> Optional.of(mainCode.get().getDisplay()));
            displayName.ifPresent(builder::mainDisplayName);

            if (codeableConcept.hasText()) {
                builder.mainOriginalText(codeableConcept.getText());
            }
        } else {
            var originalText = findOriginalText(codeableConcept, mainCode);
            originalText.ifPresent(builder::mainOriginalText);
        }

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    public String mapCodeableConceptToCdForTransformedActualProblemHeader(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();

        var originalText = CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
        originalText.ifPresent(builder::mainOriginalText);

        builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
        builder.mainCode(FIXED_ACTUAL_PROBLEM_CODE);
        builder.mainDisplayName(PROBLEM_DISPLAY_NAME);

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    private Optional<Coding> findMainCode(CodeableConcept codeableConcept) {
        return codeableConcept.getCoding()
            .stream()
            .filter(this::isSnomed)
            .findFirst();
    }

    private Optional<String> findOriginalText(CodeableConcept codeableConcept, Optional<Coding> coding) {
        if (coding.isPresent()) {
            if (codeableConcept.hasText()) {
                return Optional.of(codeableConcept.getText());
            } else {
                var extension = retrieveDescriptionExtension(coding.get());
                Optional<String> originalText = extension.stream()
                    .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                    .map(extension1 -> extension1.getValue().toString())
                    .findFirst();

                if (originalText.isEmpty() && coding.get().hasDisplay()) {
                    originalText = Optional.of(coding.get().getDisplay());
                }

                return originalText;
            }
        } else {
            return CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
        }
    }

    private Optional<String> findDisplayText(Coding coding) {
        return Optional.of(coding.getDisplay());
    }

    private boolean isSnomed(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM);
    }

    private Optional<Extension> retrieveDescriptionExtension(Coding coding) {
        return coding
            .getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(DESCRIPTION_URL))
            .findFirst();
    }

    public String getDisplayFromCodeableConcept(CodeableConcept codeableConcept) {
        return findMainCode(codeableConcept)
            .map(cc -> findDisplayText(cc).orElse(StringUtils.EMPTY))
            .orElse(StringUtils.EMPTY);
    }

    public String mapToNullFlavorCodeableConcept(CodeableConcept codeableConcept) {

        var builder = CodeableConceptCdTemplateParameters.builder().nullFlavor(true);
        var mainCode = findMainCode(codeableConcept);

        var originalText = findOriginalText(codeableConcept, mainCode);
        originalText.ifPresent(builder::mainOriginalText);
        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }
}
