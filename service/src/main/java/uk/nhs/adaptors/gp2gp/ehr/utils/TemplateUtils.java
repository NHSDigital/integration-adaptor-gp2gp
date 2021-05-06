package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import org.apache.commons.lang3.StringUtils;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class TemplateUtils {
    private static final String TEMPLATES_DIRECTORY = "templates";

    public static Mustache loadTemplate(String templateName) {
        MustacheFactory mustacheFactory = new XmlMustacheFactory(TEMPLATES_DIRECTORY);
        return mustacheFactory.compile(templateName);
    }

    public static String fillTemplate(Mustache template, Object content) {
        StringWriter writer = new StringWriter();
        String data = StringUtils.EMPTY;

        try {
            template.execute(writer, content).flush();
            data += writer.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return data;
    }

    public static Mustache compileTemplate(String templateContent) {
        MustacheFactory mf = new DefaultMustacheFactory();
        return mf.compile(new StringReader(templateContent), "ad hoc template");
    }
}
