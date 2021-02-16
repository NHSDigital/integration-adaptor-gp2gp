package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ObservationStatementTemplateParameters {
    private String observationStatementId;
    private String comment;
    private String effectiveTime;
    private String issued;
    private boolean isNested;
}
