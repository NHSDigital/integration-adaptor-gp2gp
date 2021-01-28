package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventWrapperTemplateParameters {
    private String eventId;
    private String creationTime;
    private String fromAsid;
    private String toAsid;
    private String content;
}
