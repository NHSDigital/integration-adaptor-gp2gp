package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

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
    private String issued;
    private boolean isNested;
}
