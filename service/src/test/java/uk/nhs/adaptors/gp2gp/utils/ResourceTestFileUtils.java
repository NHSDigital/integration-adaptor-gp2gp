package uk.nhs.adaptors.gp2gp.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapperTest;

public class ResourceTestFileUtils {
    public static String getFileContent(String filePath) throws IOException {
        try (InputStream is = EhrExtractMapperTest.class.getResourceAsStream(filePath)) {
            if (is == null) {
                throw new FileNotFoundException(filePath);
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
