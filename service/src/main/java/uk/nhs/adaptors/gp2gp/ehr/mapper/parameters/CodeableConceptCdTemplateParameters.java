package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

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
}
