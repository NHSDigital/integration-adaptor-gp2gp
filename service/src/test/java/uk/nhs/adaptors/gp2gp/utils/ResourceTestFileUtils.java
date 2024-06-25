package uk.nhs.adaptors.gp2gp.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class ResourceTestFileUtils {
    @SneakyThrows
    public static String getFileContent(String filePath) {
        try (InputStream is = ResourceTestFileUtils.class.getResourceAsStream(filePath)) {
            if (is == null) {
                throw new FileNotFoundException(filePath);
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
