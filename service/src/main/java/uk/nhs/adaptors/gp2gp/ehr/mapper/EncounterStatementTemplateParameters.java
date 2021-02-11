package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EncounterStatementTemplateParameters {
    private String encounterStatementId;
    private String status;
    private String availabilityTime;
    private String effectiveTime;
}
