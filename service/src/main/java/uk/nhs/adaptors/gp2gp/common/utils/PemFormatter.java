package uk.nhs.adaptors.gp2gp.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class PemFormatter {
    private static final Pattern PEM_PATTERN = Pattern.compile("(-----[A-Z ]+-----)([^-]+)(-----[A-Z ]+-----)");
    private static final int HEADER_GROUP = 1;
    private static final int BODY_GROUP = 2;
    private static final int FOOTER_GROUP = 3;

    /**
     * Different methods of importing the certificates (application.yml, ENV, Cloud secret) can affect whitespace
     * and line delimiters. For these to be read as valid PEM files the whitespace needs to be stripped and newlines
     * included appropriately. This method parses and reformats these inputs into strings that can be read as PEM files.
     *
     * @param value the certificate or key to reform
     * @return the reformatted certificate or key
     */
    public static String format(String value) {
        Matcher matcher = PEM_PATTERN.matcher(value.strip());

        if (!matcher.matches()) {
            LOGGER.debug("Invalid certificate or key format:\n{}", value);
            throw new RuntimeException("Invalid certificate or key format");
        }

        String header = matcher.group(HEADER_GROUP).strip();
        String body = matcher.group(BODY_GROUP);
        String footer = matcher.group(FOOTER_GROUP).strip();

        body = Arrays.stream(body.split("\\s+"))
            .map(String::strip)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining("\n"));

        return String.join("\n", header, body, footer);
    }
}
