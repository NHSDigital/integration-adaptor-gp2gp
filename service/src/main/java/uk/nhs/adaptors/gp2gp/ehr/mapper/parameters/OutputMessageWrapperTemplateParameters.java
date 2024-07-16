package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutputMessageWrapperTemplateParameters {
    private String interactionId;
    private String eventId;
    private String creationTime;
    private String fromAsid;
    private String toAsid;
    private String ehrExtractContent;
}
