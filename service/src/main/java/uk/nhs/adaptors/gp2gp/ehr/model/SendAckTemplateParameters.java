package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SendAckTemplateParameters {
    private final String fromAsid;
    private final String toAsid;
    private final String creationTime;
    private final String uuid;
    private final String typeCode;
    private final String messageId;
}
