package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ParticipantType {
    PERFORMER("PRF"),
    AUTHOR("AUT");

    private final String code;
}
