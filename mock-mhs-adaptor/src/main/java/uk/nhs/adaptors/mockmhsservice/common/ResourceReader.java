package uk.nhs.adaptors.mockmhsservice.common;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;

public class ResourceReader {
    @SneakyThrows
    public static String readAsString(String resourceName) {
        try (InputStream inputStream = ResourceReader.class.getResourceAsStream(String.format("/%s",resourceName))) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
