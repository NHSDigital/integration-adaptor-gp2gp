package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {
    public static String newLine(Object... values) {
        return StringUtils.joinWith(StringUtils.LF, values);
    }

    public static String withSpace(Object... values) {
        return StringUtils.join(values, StringUtils.SPACE);
    }

    public static String withSpace(List<String> values) {
        return values.stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(StringUtils.SPACE));
    }
}
