package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.Getter;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Getter
public enum Template {
    ABSENT_ATTACHMENT("absent_attachment_template.mustache");

    private final Mustache mustacheTemplate;

    Template(String mustacheTemplate) {
        this.mustacheTemplate = TemplateUtils.loadTemplate(mustacheTemplate);
    }
}