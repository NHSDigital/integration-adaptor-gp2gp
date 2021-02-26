package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CodeableConceptCdTemplateParameters {
    private String mainCode;
    private String mainCodeSystem;
    private String mainDisplayName;
    private String mainOriginalText;
    private boolean nullFlavor;
    private List<TranslationCodes> codes;

    @Getter
    @Setter
    @Builder
    static
    public class TranslationCodes {
        private String translationCode;
        private String translationCodeSystem;
        private String translationDisplayName;
    }
}
