package uk.nhs.adaptors.gp2gp.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapperTest;

import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class ResourceTestFileUtils {
    public static String getFileContent(String filePath) throws IOException {
        return IOUtils.toString(EhrExtractMapperTest.class.getResourceAsStream(filePath), StandardCharsets.UTF_8);
    }
}
