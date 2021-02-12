package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;
import org.hl7.fhir.dstu3.model.CodeableConcept;

public class CodeableConceptMappingUtils {

    public static Optional<String> extractTextOrCoding(CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return Optional.of(codeableConcept.getText());
        } else {
            return Optional.ofNullable(codeableConcept.getCodingFirstRep().getDisplay());
        }
    }

}
