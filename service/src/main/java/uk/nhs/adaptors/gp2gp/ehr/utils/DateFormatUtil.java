package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    public static final int NINE = 9;
    public static final int EIGHT = 8;
    public static final int ONE = 1;
    public static final String ZERO_STRING = "0";
    public static final String TEN_STRING = "10";
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
                    + formatMonthElement(baseDateTimeType.getMonth());
            case DAY:
                return baseDateTimeType.getYear()
                    + formatMonthElement(baseDateTimeType.getMonth())
                    + formatDayElement(baseDateTimeType.getDay());
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
                    + formatMonthElement(baseDateTimeType.getMonth());
            case DAY:
                return baseDateTimeType.getYear() + "-"
                    + formatMonthElement(baseDateTimeType.getMonth())
                    + "-" + formatDayElement(baseDateTimeType.getDay());
            default:
                return baseDateTimeType.toCalendar().toInstant().atZone(UK_ZONE_ID)
                    .format(HL7_SECONDS_HUMAN_READABLE);
        }
    }

    private static String formatMonthElement(int element) {
        if (element <= EIGHT) {
            return ZERO_STRING + (element + ONE);
        } else if (element == NINE) {
            return TEN_STRING;
        }
        return String.valueOf(element);
    }

    private static String formatDayElement(int element) {
        if (element <= NINE) {
            return ZERO_STRING + element;
        }
        return String.valueOf(element);
    }
}
