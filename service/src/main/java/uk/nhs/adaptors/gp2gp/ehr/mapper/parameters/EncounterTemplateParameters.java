package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EncounterTemplateParameters {
    private String encounterStatementId;
    private String status;
    private String availabilityTime;
    private String effectiveTime;
    private String components;
}
