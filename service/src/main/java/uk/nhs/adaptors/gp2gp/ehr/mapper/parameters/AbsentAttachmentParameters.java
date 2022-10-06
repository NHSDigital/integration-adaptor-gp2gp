package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AbsentAttachmentParameters {

    private String originalFilename;
    private String odsCode;
    private String conversationId;

}
