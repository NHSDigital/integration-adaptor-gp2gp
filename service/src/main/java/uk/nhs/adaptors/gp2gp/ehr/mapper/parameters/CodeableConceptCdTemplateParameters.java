package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.ehr.mapper.TranslationCodes;

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
}
