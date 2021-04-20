package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ParticipantCoding {
    RECORDER("REC");

    private final String coding;
}
