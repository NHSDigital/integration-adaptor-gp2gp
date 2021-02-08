package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

public class DateFormatUtil {
    private static final String UK_ZONE_ID = "Europe/London";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();

    public static String formatDate(Date date) {
        return DATE_TIME_FORMATTER.format(
            date
                .toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }
}
