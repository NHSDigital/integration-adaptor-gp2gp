package uk.nhs.adaptors.gp2gp.mhs.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MhsPayloadTemplateParameters {
    private String messageId;
    private String resourceCreated;
    private String fromAsid;
    private String toAsid;
    private String fromOdsCode;
    private String toOdsCode;
    private String accessDocumentId;
}
