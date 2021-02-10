package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@Builder
public class ObservationStatementTemplateParameters {
    private String observationStatementId;
    private String comment;
    private String effectiveTime;
    private String effectiveTimeLow;
    private String effectiveTimeHigh;
    private String issued;
    private boolean isNested;
}
