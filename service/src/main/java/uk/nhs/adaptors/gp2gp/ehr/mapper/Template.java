package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

public enum Template {
    ABSENT_ATTACHMENT("absent_attachment_template.mustache");

    public final Mustache mustacheTemplate;

    Template(String mustacheTemplate) {
        this.mustacheTemplate = TemplateUtils.loadTemplate(mustacheTemplate);
    }
}