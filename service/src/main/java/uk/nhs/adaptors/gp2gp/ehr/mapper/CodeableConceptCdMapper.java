package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
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
    private static final String DESCRIPTION_DISPLAY = "descriptionDisplay";
    private static final String DESCRIPTION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";
    private static final String FIXED_ACTUAL_PROBLEM_CODE = "55607006";
    private static final String PROBLEM_DISPLAY_NAME = "Problem";
    private static final String ACTIVE_CLINICAL_STATUS = "active";

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

    public String mapCodeableConceptToCdForAllergy(CodeableConcept codeableConcept, AllergyIntolerance.AllergyIntoleranceClinicalStatus
        allergyIntoleranceClinicalStatus) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = findMainCode(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());
        var originalText = findOriginalTextForAllergy(codeableConcept, mainCode, allergyIntoleranceClinicalStatus);
        originalText.ifPresent(builder::mainOriginalText);

        if (mainCode.isPresent()) {
            if (ACTIVE_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
                builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
            } else {
                builder.nullFlavor(true);
            }

            var code = extractCode(mainCode.get());
            var displayText = findDisplayText(mainCode.get());

            code.ifPresent(builder::mainCode);
            displayText.ifPresent(builder::mainDisplayName);
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
                if (coding.get().hasDisplay()) {
                    Optional<String> originalText = Optional.of(coding.get().getDisplay());
                    return originalText;
                } else {
                    var extension = retrieveDisplayExtension(coding.get());
                    Optional<String> originalText = extension.stream()
                        .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                        .map(extension1 -> extension1.getValue().toString())
                        .findFirst();
                    return originalText;
                }
            }
        } else {
            return CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
        }
    }

    private Optional<String> findOriginalTextForAllergy(CodeableConcept codeableConcept, Optional<Coding> coding,
        AllergyIntolerance.AllergyIntoleranceClinicalStatus allergyIntoleranceClinicalStatus) {

        if (!allergyIntoleranceClinicalStatus.toCode().isEmpty()) {
            if (!ACTIVE_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
                if (coding.isPresent()) {
                    if (codeableConcept.hasText()) {
                        return Optional.of(codeableConcept.getText());
                    } else {
                        var extension = retrieveDisplayExtension(coding.get());
                        Optional<String> originalText = extension.get().getExtension().stream()
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
            } else {
                var extension = retrieveDisplayExtension(coding.get());
                Optional<String> originalText = extension.get().getExtension().stream()
                    .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                    .map(extension1 -> extension1.getValue().toString())
                    .findFirst();

                if (originalText.isEmpty()) {
                    return Optional.empty();
                }

                return originalText;
            }
        }
        throw new EhrMapperException("Allergy code not present");
    }

    private Optional<String> extractCode(Coding coding) {
        return Optional.of(coding.getCode());
    }

    private Optional<String> findDisplayText(Coding coding) {
        return Optional.of(coding.getDisplay());
    }

    private boolean isSnomed(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM);
    }

    private Optional<Extension> retrieveDisplayExtension(Coding coding) {
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

    public String mapToNullFlavorCodeableConceptForAllergy(CodeableConcept codeableConcept,
        AllergyIntolerance.AllergyIntoleranceClinicalStatus allergyIntoleranceClinicalStatus) {

        var builder = CodeableConceptCdTemplateParameters.builder().nullFlavor(true);
        var mainCode = findMainCode(codeableConcept);

        var originalText = findOriginalTextForAllergy(codeableConcept, mainCode, allergyIntoleranceClinicalStatus);
        originalText.ifPresent(builder::mainOriginalText);
        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }
}
