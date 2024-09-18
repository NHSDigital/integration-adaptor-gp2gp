package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AbsentAttachmentParameters;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.Template.ABSENT_ATTACHMENT;

@Component
public final class AbsentAttachmentFileMapper {
    public String mapFileDataToAbsentAttachment(String originalFilename, String odsCode, String conversationId) {
        final AbsentAttachmentParameters absentAttachment = AbsentAttachmentParameters.builder()
            .originalFilename(originalFilename)
            .odsCode(odsCode)
            .conversationId(conversationId)
            .build();

        return TemplateUtils.fillTemplate(ABSENT_ATTACHMENT.getMustacheTemplate(), absentAttachment);
    }
}