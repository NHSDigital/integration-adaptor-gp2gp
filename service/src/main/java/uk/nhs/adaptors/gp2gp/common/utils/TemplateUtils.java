package uk.nhs.adaptors.gp2gp.common.utils;

public class TemplatesUtils {
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
            e.printStackTrace();
        }

        return data;
    }
}
