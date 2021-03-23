package uk.nhs.adaptors.mockmhsservice.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.SneakyThrows;

public class ResourceReader {
    @SneakyThrows
    public static String readAsString(String resourceName) {
        try (InputStream inputStream = ResourceReader.class.getResourceAsStream(String.format("/%s",resourceName))) {
            var resourceAsString = IOUtils.toString(inputStream, UTF_8);
            inputStream.close();
            return resourceAsString;
        }
    }
}
