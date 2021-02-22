package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@Builder
public class RequestStatementTemplateParameters {
    private boolean isNested;
    private String requestStatementId;
    private String availabilityTime;
    private String description;
    private String defaultReasonCode;
}
