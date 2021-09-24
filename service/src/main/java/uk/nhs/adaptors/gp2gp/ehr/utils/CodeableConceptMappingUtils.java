package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;

public class CodeableConceptMappingUtils {

    private static final String DISPLAY_EXTENSION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";
    private static final String DESCRIPTION_DISPLAY_URL = "descriptionDisplay";

    public static Optional<String> extractTextOrCoding(CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return Optional.of(codeableConcept.getText());
        } else {
            return Optional.ofNullable(codeableConcept.getCoding())
                .stream()
                .flatMap(List::stream)
                .findFirst()
                .map(Coding.class::cast)
                .map(coding -> Optional.ofNullable(getExtensionTermText(coding.getExtension()))
                    .filter(value -> StringUtils.isNotBlank(value) && !value.equals(coding.getDisplay()))
                    .orElseGet(coding::getDisplay));
        }
    }

    public static Optional<String> extractUserSelectedTextOrCoding(CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return Optional.of(codeableConcept.getText());
        } else {
            return codeableConcept.getCoding().stream()
                .filter(Coding::hasUserSelected)
                .findFirst()
                .map(Coding.class::cast)
                .map(coding -> Optional.ofNullable(getExtensionTermText(coding.getExtension()))
                    .filter(value -> StringUtils.isNotBlank(value) && !value.equals(coding.getDisplay()))
                    .orElseGet(coding::getDisplay))
                .or(() -> CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept));
        }
    }

    public static boolean hasCode(CodeableConcept code, List<String> codeLists) {
        return code != null && code.getCoding()
            .stream()
            .map(Coding::getCode)
            .anyMatch(codeLists::contains);
    }
    
    private static String getExtensionTermText(List<Extension> extensions){
        return extensions.stream()
            .filter(extension -> extension.getUrl().equals(DISPLAY_EXTENSION_URL))
            .map(Extension.class::cast)
            .map(Extension::getExtension)
            .flatMap(List::stream)
            .filter(extension -> extension.getUrl().equals(DESCRIPTION_DISPLAY_URL))
            .findFirst()
            .map(value -> value.getValue().toString())
            .orElse(StringUtils.EMPTY);
    }
}
