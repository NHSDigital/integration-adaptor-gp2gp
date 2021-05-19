package uk.nhs.adaptors.gp2gp;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import lombok.SneakyThrows;

public class XsdValidator {

    @SneakyThrows
    public static void validateFileContentAgainstSchema(String fileContent) {
        try (var inputStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))) {
            Schema schema = getSchemaFromUrl();
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(inputStream));
        }
    }

    @SneakyThrows
    private static Schema getSchemaFromUrl() {
        URL schemaURL = XsdValidator.class.getResource("/mim/Schemas/RCMR_IN030000UK06.xsd");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(schemaURL);
    }

}
