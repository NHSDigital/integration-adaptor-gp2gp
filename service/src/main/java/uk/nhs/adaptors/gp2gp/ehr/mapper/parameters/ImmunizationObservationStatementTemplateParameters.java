package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ImmunizationObservationStatementTemplateParameters {
    private boolean isNested;
    private String observationStatementId;
    private String availabilityTime;
    private String confidentialityCode;
    private String effectiveTime;
    private String pertinentInformation;
    private String code;
    private String participant;
}
