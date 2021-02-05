package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.io.IOException;
import java.io.Writer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import org.apache.commons.text.StringEscapeUtils;

public class XmlMustacheFactory extends DefaultMustacheFactory {
    public XmlMustacheFactory(String resourceRoot) {
        super(resourceRoot);
    }

    @Override
    public void encode(String value, Writer writer) {
        try {
            writer.write(StringEscapeUtils.escapeXml10(value));
        } catch (IOException e) {
            throw new MustacheException("Failed to encode value: " + value, e);
        }
    }
}
