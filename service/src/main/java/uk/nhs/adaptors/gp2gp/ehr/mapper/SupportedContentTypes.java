package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

@Component
public class SupportedContentTypes {
    private static final String SUPPORTED_CONTENT_TYPES_PATH = "attachments/supported-content-types.txt";

    private final List<String> supportedTypes;

    public SupportedContentTypes() {
        try {
            URL resource = SupportedContentTypes.class.getClassLoader().getResource(SUPPORTED_CONTENT_TYPES_PATH);
            if (resource != null) {
                supportedTypes = Files.readAllLines(Paths.get(resource.toURI()));
            } else {
                throw new EhrMapperException("File does not exist. Unable to load supported content-types from location: "
                    + SUPPORTED_CONTENT_TYPES_PATH);
            }
        } catch (IOException | URISyntaxException e) {
            throw new EhrMapperException("Unable to load supported content-types from location: " + SUPPORTED_CONTENT_TYPES_PATH, e);
        }
    }

    public boolean isContentTypeSupported(String attachmentContentType) {
        return !CollectionUtils.isEmpty(supportedTypes) && supportedTypes.contains(attachmentContentType);
    }
}
