package uk.nhs.adaptors.gp2gp.ehr.utils;

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

    private static final DateTimeFormatter TEXT_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .toFormatter();

    public static String formatDate(Date date) {
        return format(date, DATE_TIME_FORMATTER);
    }

    public static Object formatTextDate(Date date) {
        return format(date, TEXT_DATE_TIME_FORMATTER);
    }

    private static String format(Date date, DateTimeFormatter dateTimeFormatter) {
        if (date == null) {
            throw new EhrMapperException("Could not format date");
        }

        return dateTimeFormatter.format(
            date.toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }
}
