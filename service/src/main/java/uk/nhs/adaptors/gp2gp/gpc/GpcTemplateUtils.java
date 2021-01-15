package uk.nhs.adaptors.gp2gp.gpc;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.IOException;
import java.io.StringWriter;

public class GpcTemplateUtils {

    private static final String TEMPLATES_DIRECTORY = "templates";

    public static Mustache loadTemplate(String templateName) {
        MustacheFactory mf = new DefaultMustacheFactory(TEMPLATES_DIRECTORY);
        Mustache m = mf.compile(templateName);
        return m;
    }

    public static String fillTemplate(Mustache template, Object content) {
        StringWriter writer = new StringWriter();
        String data = "";

        try {
            template.execute(writer, content).flush();
            data += writer.toString();
        } catch (IOException e) {
            throw new GpcException("Unable to create the JWT token for the Authorization header. Exception: ", e);
        }

        return data;
    }
}
