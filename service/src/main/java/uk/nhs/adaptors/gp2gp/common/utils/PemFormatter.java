package uk.nhs.adaptors.gp2gp.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PemFormatter {
    private static final Pattern PEM_PATTERN = Pattern.compile("(-----[A-Z ]+-----)([^-]+)(-----[A-Z ]+-----)");
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
            throw new RuntimeException("Invalid certificate or key format");
        }

        String header = matcher.group(1).strip();
        String body = matcher.group(2);
        String footer = matcher.group(3).strip();

        body = Arrays.stream(body.split("\\s+"))
            .map(String::strip)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining("\n"));

        return String.join("\n", header, body, footer);
    }
}
