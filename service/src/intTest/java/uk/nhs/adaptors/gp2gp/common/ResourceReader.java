package uk.nhs.adaptors.gp2gp.common;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ResourceReader {
    public static String asString(Resource resource) {
        try {
            return IOUtils.toString(resource.getInputStream(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}