package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EhrDocumentTemplateParameters {
    private String messageId;
    private String resourceCreated;
    private String fromAsid;
    private String toAsid;
    private String fromOdsCode;
    private String toOdsCode;
    private String accessDocumentId;
    private String pertinentPayloadId;
}
