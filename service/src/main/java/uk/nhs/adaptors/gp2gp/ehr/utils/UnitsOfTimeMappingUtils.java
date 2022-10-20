package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Map;

public class UnitsOfTimeMappingUtils {
    private static final Map<String, String> UNITS_OF_TIME = Map.of(
        "s", "seconds",
        "min", "minute",
        "h", "hours",
        "d", "days",
        "wk", "weeks",
        "mo", "months",
        "a", "years"
    );

    public static String mapCodeToDisplayValue(String code) {
        return UNITS_OF_TIME.get(code);
    }
}
