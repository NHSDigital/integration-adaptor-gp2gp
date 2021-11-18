package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AbsentAttachmentFileMapper {

    private static final Mustache ABSENT_ATTACHMENT_TEMPLATE = TemplateUtils.loadTemplate("absent_attachment_template.mustache");

    public static String mapDataToAbsentAttachment(String title, String odsCode, String conversationId) {
        var absentAttachment = AbsentAttachment.builder()
            .title(title) //pass the value of content.attachment.title got from Emis
            .odsCode(odsCode)  //transaction or wrapper layer
            .conversationId(conversationId) //transaction or wrapper layer
            .build();
        return TemplateUtils.fillTemplate(ABSENT_ATTACHMENT_TEMPLATE, absentAttachment);
    }


    //TODO: Create a uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AbsentAttachmentParameters class if needed
    // This one's only draft
    @Builder
    private static class AbsentAttachment {
        private String title; //content.attachment.title
        private String odsCode;
        private String conversationId;
    }

}