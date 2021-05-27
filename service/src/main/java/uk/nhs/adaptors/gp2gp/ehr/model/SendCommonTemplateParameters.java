package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SendCommonTemplateParameters {
    private final String fromAsid;
    private final String document;
}
