package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final int MONTH_PADDING = 1;
    private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");
    private static final String COULD_NOT_FORMAT_DATE = "Could not format date";
    private static final DateTimeFormatter HL7_SECONDS_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter HL7_SECONDS_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String FORMAT_TWO_DIGITS = "%02d";

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
                    + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getMonth() + MONTH_PADDING);
            case DAY:
                return baseDateTimeType.getYear()
                    + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getMonth() + MONTH_PADDING)
                    + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getDay());
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
                    + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getMonth() + MONTH_PADDING);
            case DAY:
                return baseDateTimeType.getYear() + "-"
                    + String.format(FORMAT_TWO_DIGITS,  baseDateTimeType.getMonth() + MONTH_PADDING)
                    + "-" + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getDay());
            default:
                return baseDateTimeType.toCalendar().toInstant().atZone(UK_ZONE_ID)
                    .format(HL7_SECONDS_HUMAN_READABLE);
        }
    }
}
