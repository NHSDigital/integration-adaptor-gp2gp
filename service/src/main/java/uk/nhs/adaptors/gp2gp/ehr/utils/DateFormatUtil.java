package uk.nhs.adaptors.gp2gp.ehr.utils;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.DAY;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MINUTE;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MONTH;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.YEAR;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

import java.time.LocalDateTime;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.InstantType;

import com.google.common.collect.ImmutableMap;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final String UK_ZONE_ID = "Europe/London";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();
    private static final String SHORT_DATE_FORMAT = "yyyy-MM-dd";
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

    public static String formatShortDate(Date date) {
        return new SimpleDateFormat(SHORT_DATE_FORMAT).format(date);
    }

    public static String formatDate(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant.atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }

    //here

    public static final String COULD_NOT_FORMAT_DATE = "Could not format date";
    private static final ImmutableMap<TemporalPrecisionEnum, String> YEAR_PATTERN_MAP = ImmutableMap.of(
        YEAR, "yyyy",
        MONTH, "yyyyMM",
        DAY, "yyyyMMdd",
        MINUTE, "yyyyMMddHHmm"
    );

    public static String formatDateType(DateType dateType) {
        if (!dateType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return Optional.ofNullable(YEAR_PATTERN_MAP.get(dateType.getPrecision()))
            .map(pattern -> dateTypeConverter(dateType, pattern))
            .orElseGet(() -> dateTypeConverter(dateType, "yyyyMMddHHmmss"));
    }

    public static String formatInstantType(InstantType dateInstantType) {
        if (!dateInstantType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        DateTimeType dateTimeType = new DateTimeType(dateInstantType.toCalendar().toInstant().toString());

        return formatDateTimeType(dateTimeType);
    }

    public static String formatDateTimeType(DateTimeType dateTimeType) {
        if (!dateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return Optional.ofNullable(YEAR_PATTERN_MAP.get(dateTimeType.getPrecision()))
            .map(pattern -> dateTimeTypeConverter(dateTimeType, pattern))
            .orElseGet(() -> dateTimeTypeConverter(dateTimeType, "yyyyMMddHHmmss"));
    }

    private static String dateTypeConverter(DateType effectiveDateTimeType, String pattern) {
        return LocalDateTime.ofInstant(effectiveDateTimeType.toCalendar().toInstant(), ZoneId.of(UK_TIMEZONE))
            .format(DateTimeFormatter.ofPattern(pattern));
    }

    private static String dateTimeTypeConverter(DateTimeType effectiveDateTimeType, String pattern) {
        return LocalDateTime.ofInstant(effectiveDateTimeType.toCalendar().toInstant(), ZoneId.of(UK_TIMEZONE))
            .format(DateTimeFormatter.ofPattern(pattern));
    }
}
