package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final String UK_ZONE_ID = "Europe/London";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();
    private static final String SHORT_DATE_FORMAT = "yyyy-MM-dd";

    public static String formatDate(Date date) {
        if (date == null) {
            throw new EhrMapperException("Could not format date");
        }

        return DATE_TIME_FORMATTER.format(
            date.toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }

    public static String formatShortDate(Date date) {
        return new SimpleDateFormat(SHORT_DATE_FORMAT).format(date);
    }

    public static String formatDate(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant.atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }
}
