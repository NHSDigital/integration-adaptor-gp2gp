package uk.nhs.adaptors.mockmhsservice.common;

import java.nio.file.Files;
import org.springframework.util.ResourceUtils;
import lombok.SneakyThrows;

public class ResourceReader {
    @SneakyThrows
    public static String readAsString(String resourceName) {
        var file = ResourceUtils.getFile(String.format("classpath:%s", resourceName));
        return new String(Files.readAllBytes(file.toPath()));
    }
}
