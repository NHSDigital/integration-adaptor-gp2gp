package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.io.IOException;
import java.io.StringWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class TemplateUtils {
    private static final String TEMPLATES_DIRECTORY = "templates";

    public static Mustache loadTemplate(String templateName) {
        MustacheFactory mustacheFactory = new DefaultMustacheFactory(TEMPLATES_DIRECTORY);
        return mustacheFactory.compile(templateName);
    }

    public static String fillTemplate(Mustache template, Object content) {
        StringWriter writer = new StringWriter();
        String data = "";

        try {
            template.execute(writer, content).flush();
            data += writer.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return data;
    }
}
