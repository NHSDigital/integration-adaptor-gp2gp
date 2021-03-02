package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final String ZERO_STRING = "0";
    private static final String TEN_STRING = "10";
    private static final int SEPTEMBER = 8;
    private static final int OCTOBER = 9;
    private static final int MONTH_PADDING = 1;
    private static final int NINTH = 9;
    private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");
    private static final String COULD_NOT_FORMAT_DATE = "Could not format date";
    private static final DateTimeFormatter HL7_SECONDS_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter HL7_SECONDS_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String toHl7Format(Instant instant) {
        return instant.atZone(UK_ZONE_ID).format(HL7_SECONDS_COMPUTER_READABLE);
    }

    public static String toHl7Format(BaseDateTimeType baseDateTimeType) {
        if (!baseDateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        switch (baseDateTimeType.getPrecision()) {
            case YEAR:
                return baseDateTimeType.getYear().toString();
            case MONTH:
                return baseDateTimeType.getYear()
                    + formatSingleDigitMonthElement(baseDateTimeType.getMonth());
            case DAY:
                return baseDateTimeType.getYear()
                    + formatSingleDigitMonthElement(baseDateTimeType.getMonth())
                    + formatSingleDigitDayElement(baseDateTimeType.getDay());
            default:
                return baseDateTimeType.toCalendar().toInstant().atZone(UK_ZONE_ID)
                    .format(HL7_SECONDS_COMPUTER_READABLE);
        }
    }

    public static String toTextFormat(BaseDateTimeType baseDateTimeType) {
        if (!baseDateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        switch (baseDateTimeType.getPrecision()) {
            case YEAR:
                return baseDateTimeType.getYear().toString();
            case MONTH:
                return baseDateTimeType.getYear() + "-"
                    + formatSingleDigitMonthElement(baseDateTimeType.getMonth());
            case DAY:
                return baseDateTimeType.getYear() + "-"
                    + formatSingleDigitMonthElement(baseDateTimeType.getMonth())
                    + "-" + formatSingleDigitDayElement(baseDateTimeType.getDay());
            default:
                return baseDateTimeType.toCalendar().toInstant().atZone(UK_ZONE_ID)
                    .format(HL7_SECONDS_HUMAN_READABLE);
        }
    }

    private static String formatSingleDigitMonthElement(int monthValue) {
        if (monthValue <= SEPTEMBER) {
            return ZERO_STRING + (monthValue + MONTH_PADDING);
        } else if (monthValue == OCTOBER) {
            return TEN_STRING;
        }
        return String.valueOf(monthValue);
    }

    private static String formatSingleDigitDayElement(int dayValue) {
        if (dayValue <= NINTH) {
            return ZERO_STRING + dayValue;
        }
        return String.valueOf(dayValue);
    }
}
