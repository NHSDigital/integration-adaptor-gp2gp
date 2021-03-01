package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TranslationCodes {
    private String translationCode;
    private String translationCodeSystem;
    private String translationDisplayName;
}