package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

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
    private String defaultReasonCode;
    private String code;
    private String priorityCode;
    private String participant;
    private String responsibleParty;
    private String text;
}
