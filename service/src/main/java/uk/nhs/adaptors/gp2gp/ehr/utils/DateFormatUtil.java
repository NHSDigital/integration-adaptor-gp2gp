package uk.nhs.adaptors.gp2gp.ehr.utils;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.DAY;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MINUTE;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MONTH;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.YEAR;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.InstantType;

import com.google.common.collect.ImmutableMap;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final String UK_ZONE_ID = "Europe/London";
    private static final String COULD_NOT_FORMAT_DATE = "Could not format date";
    private static final String COMPUTER_READABLE_YEAR_FORMAT = "yyyy";
    private static final String COMPUTER_READABLE_MONTH_FORMAT = "yyyyMM";
    private static final String COMPUTER_READABLE_DAY_FORMAT = "yyyyMMdd";
    private static final String COMPUTER_READABLE_MINUTE_FORMAT = "yyyyMMddHHmm";
    private static final String COMPUTER_READABLE_SECOND_FORMAT = "yyyyMMddHHmmss";

    private static final DateTimeFormatter COMPUTER_READABLE_YEAR_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(COMPUTER_READABLE_YEAR_FORMAT)
        .toFormatter();
    private static final DateTimeFormatter COMPUTER_READABLE_MONTH_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(COMPUTER_READABLE_MONTH_FORMAT)
        .toFormatter();
    private static final DateTimeFormatter COMPUTER_READABLE_DAY_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(COMPUTER_READABLE_DAY_FORMAT)
        .toFormatter();
    private static final DateTimeFormatter COMPUTER_READABLE_MINUTE_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(COMPUTER_READABLE_MINUTE_FORMAT)
        .toFormatter();
    private static final DateTimeFormatter COMPUTER_READABLE_SECOND_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(COMPUTER_READABLE_SECOND_FORMAT)
        .toFormatter();

    private static final ImmutableMap<TemporalPrecisionEnum, DateTimeFormatter> COMPUTER_READABLE_FORMATTER_MAP = ImmutableMap.of(
        YEAR, COMPUTER_READABLE_YEAR_FORMATTER,
        MONTH, COMPUTER_READABLE_MONTH_FORMATTER,
        DAY, COMPUTER_READABLE_DAY_FORMATTER,
        MINUTE, COMPUTER_READABLE_MINUTE_FORMATTER
    );

    //TODO: NIAD-1082
    private static final String TEXT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter TEXT_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(TEXT_DATE_TIME)
        .toFormatter();

    public static Object formatTextDate(Date date) {
        return format(date, TEXT_DATE_TIME_FORMATTER);
    }

    private static String format(Date date, DateTimeFormatter dateTimeFormatter) {
        if (date == null) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return dateTimeFormatter.format(
            date.toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }

    public static String formatInstant(Instant instant) {
        return COMPUTER_READABLE_SECOND_FORMATTER.format(instant.atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }

    public static String formatDateTypeComputerReadable(DateType dateType) {
        if (!dateType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(dateType.getPrecision(), dateType.toCalendar());
    }

    public static String formatInstantType(InstantType dateInstantType) {
        if (!dateInstantType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(dateInstantType.getPrecision(), dateInstantType.toCalendar());
    }

    public static String formatDateTimeTypeComputerReadable(DateTimeType dateTimeType) {
        if (!dateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(dateTimeType.getPrecision(), dateTimeType.toCalendar());
    }

    private static DateTimeFormatter getFormatStringForPrecision(TemporalPrecisionEnum precisionEnum) {
        return COMPUTER_READABLE_FORMATTER_MAP.getOrDefault(precisionEnum, COMPUTER_READABLE_SECOND_FORMATTER);
    }

    private static String convertWithPrecision(TemporalPrecisionEnum precisionEnum, Calendar calendar) {
        return LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of(UK_ZONE_ID))
            .format(getFormatStringForPrecision(precisionEnum));
    }
}
