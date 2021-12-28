package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AbsentAttachmentParameters {

    private String title;
    private String odsCode;
    private String conversationId;

}
