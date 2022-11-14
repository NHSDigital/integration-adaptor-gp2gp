package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Collections;
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
    private static final String CARE_CONNECT_PRESCRIBING_AGENCY_SYSTEM = "https://fhir.nhs.uk/STU3/CodeSystem/CareConnect-PrescribingAgency-1";
    private static final String SNOMED_SYSTEM_CODE = "2.16.840.1.113883.2.1.3.2.4.15";
    private static final String DESCRIPTION_ID = "descriptionId";
    private static final String DESCRIPTION_DISPLAY = "descriptionDisplay";
    private static final String DESCRIPTION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";
    private static final String FIXED_ACTUAL_PROBLEM_CODE = "55607006";
    private static final String PROBLEM_DISPLAY_NAME = "Problem";
    private static final String ACTIVE_CLINICAL_STATUS = "active";
    private static final String RESOLVED_CLINICAL_STATUS = "resolved";
    private static final String PRESCRIBING_AGENCY_GP_PRACTICE_CODE = "prescribed-at-gp-practice";
    private static final String PRESCRIBING_AGENCY_PREVIOUS_PRACTICE_CODE = "prescribed-by-previous-practice";
    private static final String PRESCRIBING_AGENCY_ANOTHER_ORGANISATION_CODE = "prescribed-by-another-organisation";
    private static final String EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_CODE = "394823007";
    private static final String EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_DISPLAY = "NHS Prescription";
    private static final String EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_CODE = "394828003";
    private static final String EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_DISPLAY = "Prescription by another organisation";

    public String mapCodeableConceptToCd(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = findMainCode(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());

        if (mainCode.isPresent()) {
            var extension = retrieveDescriptionExtension(mainCode.get())
                .map(Extension::getExtension)
                .orElse(Collections.emptyList());

            builder.mainCodeSystem(SNOMED_SYSTEM_CODE);

            Optional<String> code = extension.stream()
                .filter(descriptionExt -> DESCRIPTION_ID.equals(descriptionExt.getUrl()))
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

    public String mapCodeableConceptToCdForAllergy(CodeableConcept codeableConcept, AllergyIntolerance.AllergyIntoleranceClinicalStatus
        allergyIntoleranceClinicalStatus) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = findMainCode(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());

        if (mainCode.isPresent()) {
            var extension = retrieveDescriptionExtension(mainCode.get())
                .map(Extension::getExtension)
                .orElse(Collections.emptyList());

            if (ACTIVE_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
                builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
            } else {
                builder.nullFlavor(true);
            }

            Optional<String> code = extension.stream()
                .filter(descriptionExt -> DESCRIPTION_ID.equals(descriptionExt.getUrl()))
                .map(description -> description.getValue().toString())
                .findFirst()
                .or(() -> Optional.of(mainCode.get().getCode()));
            code.ifPresent(builder::mainCode);

            Optional<String> displayName = Optional.of(mainCode.get().getDisplay());
            displayName.ifPresent(builder::mainDisplayName);

            if (codeableConcept.hasText()) {
                builder.mainOriginalText(codeableConcept.getText());
            } else {
                var originalText = findOriginalTextForAllergy(codeableConcept, mainCode, allergyIntoleranceClinicalStatus);
                originalText.ifPresent(builder::mainOriginalText);
            }
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

    public String mapCodeableConceptToCdForBloodPressure(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = findMainCode(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());
        var originalText = findOriginalText(codeableConcept, mainCode);
        originalText.ifPresent(builder::mainOriginalText);

        if (mainCode.isPresent()) {
            builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
            var code = Optional.of(mainCode.get().getCode());
            var displayText = findDisplayText(mainCode.get());

            code.ifPresent(builder::mainCode);
            displayText.ifPresent(builder::mainDisplayName);
        }

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    public Optional<String> mapCodeableConceptToCdForEhrSupplyType(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var prescribingAgency = findPrescribingAgency(codeableConcept);
        String code;
        String displayText;

        if (prescribingAgency.isEmpty()) {
            return Optional.empty();
        }

        switch (prescribingAgency.orElseThrow().getCode()) {
            case PRESCRIBING_AGENCY_GP_PRACTICE_CODE:
            case PRESCRIBING_AGENCY_PREVIOUS_PRACTICE_CODE:
                code = EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_CODE;
                displayText = EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_DISPLAY;
                break;
            case PRESCRIBING_AGENCY_ANOTHER_ORGANISATION_CODE:
                code = EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_CODE;
                displayText = EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_DISPLAY;
                break;
            default:
                return Optional.empty();
        }

        builder
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainCode(code)
            .mainDisplayName(displayText);

        return Optional.of(TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build()));
    }

    private Optional<Coding> findPrescribingAgency(CodeableConcept codeableConcept) {
        return codeableConcept.getCoding()
            .stream()
            .filter(this::isPrescribingAgency)
            .findFirst();
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
                    var extension = retrieveDescriptionExtension(coding.get());
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
            if (RESOLVED_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
                if (coding.isPresent()) {
                    if (codeableConcept.hasText()) {
                        return Optional.of(codeableConcept.getText());
                    } else {
                        var extension = retrieveDescriptionExtension(coding.get());
                        if (extension.isPresent()) {
                            Optional<String> originalText = extension
                                .get()
                                .getExtension().stream()
                                .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                                .map(extension1 -> extension1.getValue().toString())
                                .findFirst();

                            if (originalText.isPresent()) {
                                return originalText;
                            } else if (coding.get().hasDisplay()) {
                                return Optional.of(coding.get().getDisplay());
                            }
                        } else if (coding.get().hasDisplay()) {
                            return Optional.of(coding.get().getDisplay());
                        }
                    }
                }
            } else if (ACTIVE_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
                Optional<Extension> extension = retrieveDescriptionExtension(coding.get());
                if (extension.isPresent()) {
                    Optional<String> originalText = extension
                        .get()
                        .getExtension().stream()
                        .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                        .map(extension1 -> extension1.getValue().toString())
                        .findFirst();
                    if (originalText.isPresent() && StringUtils.isNotBlank(originalText.get())) {
                        return originalText;
                    }
                }

                return Optional.empty();
            }
        }

        return CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
    }

    private Optional<String> findDisplayText(Coding coding) {
        return Optional.of(coding.getDisplay());
    }

    private boolean isSnomed(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM);
    }

    private boolean isPrescribingAgency(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(CARE_CONNECT_PRESCRIBING_AGENCY_SYSTEM);
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

    public String mapToNullFlavorCodeableConceptForAllergy(CodeableConcept codeableConcept,
        AllergyIntolerance.AllergyIntoleranceClinicalStatus allergyIntoleranceClinicalStatus) {

        var builder = CodeableConceptCdTemplateParameters.builder().nullFlavor(true);
        var mainCode = findMainCode(codeableConcept);

        var originalText = findOriginalTextForAllergy(codeableConcept, mainCode, allergyIntoleranceClinicalStatus);
        originalText.ifPresent(builder::mainOriginalText);
        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }
}
