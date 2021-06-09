package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class SupportedContentTypes {
    private static final String SUPPORTED_CONTENT_TYPES_PATH = "supported-content-types.txt";
    private static final Set<String> SUPPORTED_CONTENT_TYPES = loadSupportedContentTypes();

    public boolean isContentTypeSupported(String attachmentContentType) {
        return SUPPORTED_CONTENT_TYPES.contains(attachmentContentType);
    }

    @SneakyThrows
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
        justification = "https://github.com/spotbugs/spotbugs/issues/1338")
    private static Set<String> loadSupportedContentTypes() {
        try (InputStream is = SupportedContentTypes.class.getClassLoader().getResourceAsStream(SUPPORTED_CONTENT_TYPES_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            return reader.lines()
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toSet());
        }
    }
}
