package uk.nhs.adaptors.gp2gp;

import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.ehr.XPathService;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ResourceHelper {
    public static String loadClasspathResourceAsString(String path) {
        return new Scanner(ResourceHelper.class.getResourceAsStream(path), StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }

    public static Document loadClasspathResourceAsXml(String path) {
        return new XPathService().prepareDocumentFromXml(loadClasspathResourceAsString(path));
    }
}