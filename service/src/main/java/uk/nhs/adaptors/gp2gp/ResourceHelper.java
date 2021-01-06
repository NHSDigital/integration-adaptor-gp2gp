package uk.nhs.adaptors.gp2gp;

import lombok.SneakyThrows;
import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ResourceHelper {
    public static String loadClasspathResourceAsString(String path) {
        return new Scanner(ResourceHelper.class.getResourceAsStream(path), StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }

    @SneakyThrows
    public static Document loadClasspathResourceAsXml(String path) {
        return new XPathService().prepareDocumentFromXml(loadClasspathResourceAsString(path));
    }
}