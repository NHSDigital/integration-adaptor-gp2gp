package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AbsentAttachmentParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AbsentAttachmentFileMapper {

    private static final Mustache ABSENT_ATTACHMENT_TEMPLATE = TemplateUtils.loadTemplate("absent_attachment_template.mustache");

    public static String mapDataToAbsentAttachment(String originalFilename, String odsCode, String conversationId) {
        var absentAttachment = AbsentAttachmentParameters.builder()
            .originalFilename(originalFilename)
            .odsCode(odsCode)
            .conversationId(conversationId)
            .build();
        return TemplateUtils.fillTemplate(ABSENT_ATTACHMENT_TEMPLATE, absentAttachment);
    }

}
