package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;

import com.github.mustachejava.Mustache;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.CodeableConceptCdTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Slf4j
public class CodeableConceptCdMapper {

    private static final Mustache CODEABLE_CONCEPT_CD_TEMPLATE = TemplateUtils
        .loadTemplate("codeable_concept_cd_template.mustache");
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String NO_CODE_SYSTEM_FOUND = "No valid code system found";
    private static final String DESCRIPTION_DISPLAY = "descriptionDisplay";
    private static final String DESCRIPTION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";
    private static final Map<String, String> FHIR_CODE_TO_HL7_OID = Map.of(
        "http://snomed.info/sct", "2.16.840.1.113883.2.1.3.2.4.15",
        "http://read.info/readv2", "2.16.840.1.113883.2.1.6.2",
        "http://read.info/ctv3", "2.16.840.1.113883.2.1.3.2.4.14"
    );

    public String mapCodeableConceptToCd(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        Optional<Coding> mainCode;
        var firstFoundMainCode = findMainCode(codeableConcept);

        if (firstFoundMainCode != null && isCodingSystemValid(firstFoundMainCode)) {
            setMainCodeProperties(builder, codeableConcept, firstFoundMainCode);
            mainCode = Optional.of(firstFoundMainCode);

        } else {
            var backupMainCode = findCodeWithValidSystem(codeableConcept, firstFoundMainCode);
            backupMainCode.ifPresentOrElse(coding -> setMainCodeProperties(builder, codeableConcept, coding),
                () -> setMainCodePropertiesWithNullFlavour(builder, firstFoundMainCode, codeableConcept, true));
            mainCode = backupMainCode;
        }

        if (mainCode.isPresent()) {
            var translationCodes = buildTranslationCodes(codeableConcept, mainCode.get());
            builder.codes(translationCodes);
        }

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    private Coding findMainCode(CodeableConcept codeableConcept) {
        var priorityCode = codeableConcept.getCoding()
            .stream()
            .filter(this::isUserSelected)
            .filter(this::isCodingSystemValid)
            .findFirst()
            .or(() -> codeableConcept.getCoding()
                .stream()
                .filter(this::isSnomed)
                .findFirst()
            );

        return priorityCode.orElse(codeableConcept.getCodingFirstRep());
    }

    private Optional<Coding> findCodeWithValidSystem(CodeableConcept codeableConcept, Coding excludedCode) {
        return codeableConcept.getCoding()
            .stream()
            .filter(coding -> !coding.equals(excludedCode))
            .filter(this::isCodingSystemValid)
            .findFirst();
    }

    private void setMainCodeProperties(CodeableConceptCdTemplateParameters.CodeableConceptCdTemplateParametersBuilder builder,
        CodeableConcept codeableConcept, Coding mainCode) {
        setMainCodePropertiesWithNullFlavour(builder, mainCode, codeableConcept, false);
    }

    private void setMainCodePropertiesWithNullFlavour(CodeableConceptCdTemplateParameters
        .CodeableConceptCdTemplateParametersBuilder builder, Coding mainCode, CodeableConcept codeableConcept, boolean nullFlavor) {

        var codingSystem = extractCodingSystem(mainCode);
        var code = extractCode(mainCode);
        var displayText = extractDisplayText(mainCode);

        var originalText = extractOriginalText(mainCode, codeableConcept);
        originalText.ifPresent(builder::mainOriginalText);

        codingSystem.ifPresent(builder::mainCodeSystem);
        code.ifPresent(builder::mainCode);
        displayText.ifPresent(builder::mainDisplayName);
        builder.nullFlavor(nullFlavor);
    }

    private Optional<String> extractCodingSystem(Coding coding) {
        if (coding.hasSystem()) {
            return Optional.of(FHIR_CODE_TO_HL7_OID.getOrDefault(coding.getSystem(), StringUtils.EMPTY));
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractCode(Coding coding) {
        return Optional.of(coding.getCode());
    }

    private Optional<String> extractDisplayText(Coding coding) {
        return Optional.of(coding.getDisplay());
    }

    private Optional<String> extractOriginalText(Coding coding, CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return Optional.of(codeableConcept.getText());
        } else if (isSnomed(coding)) {
            return extractDescriptionDisplay(coding).map(this::extractExtensionValue).orElse(Optional.of(StringUtils.EMPTY));
        }
        return  Optional.empty();
    }

    private boolean isUserSelected(Coding coding) {
        return coding.hasUserSelected() && coding.getUserSelected();
    }

    private boolean isSnomed(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equalsIgnoreCase(SNOMED_SYSTEM);
    }

    private Optional<Extension> extractDescriptionDisplay(Coding coding) {
        return coding.getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equalsIgnoreCase(DESCRIPTION_URL))
            .findFirst();
    }

    private Optional<String> extractExtensionValue(Extension extension) {
        return extension.getExtension()
            .stream()
            .filter(extension1 -> extension1.getUrl().equals(DESCRIPTION_DISPLAY))
            .map(value -> value.getValue().toString())
            .findFirst();
    }

    private boolean isCodingSystemValid(Coding mainCode) {
        if (mainCode.hasSystem()) {
            var system = FHIR_CODE_TO_HL7_OID.getOrDefault(mainCode.getSystem(), NO_CODE_SYSTEM_FOUND);
            return !system.equals(NO_CODE_SYSTEM_FOUND);
        }
        return false;
    }

    private List<TranslationCodes> buildTranslationCodes(CodeableConcept codeableConcept,
        Coding mainCode) {
        return codeableConcept.getCoding()
            .stream()
            .filter(coding -> !coding.equals(mainCode))
            .map(this::mapToTranslationCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private TranslationCodes mapToTranslationCode(Coding coding) {
        var translationCodeBuilder = TranslationCodes.builder();
        var codingSystem = extractCodingSystem(coding);
        var code = extractCode(coding);
        var displayName = extractDisplayText(coding);

        if (codingSystem.isPresent()) {
            codingSystem.ifPresent(translationCodeBuilder::translationCodeSystem);
            code.ifPresent(translationCodeBuilder::translationCode);
            displayName.ifPresent(translationCodeBuilder::translationDisplayName);

            return translationCodeBuilder.build();
        } else {
            LOGGER.error("Failed to find recognized code system mapping translation code");
            return null;
        }
    }
}
