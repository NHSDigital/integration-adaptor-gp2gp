package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DateTimeType;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final int MONTH_PADDING = 1;
    private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");
    private static final String COULD_NOT_FORMAT_DATE = "Could not format date";
    private static final String HL7_DATETIME_FORMAT = "yyyyMMddHHmmss";
    private static final String HL7_DATE_FORMAT = "yyyyMMdd";
    private static final String HL7_DATE_MONTH_FORMAT = "yyyyMM";
    private static final String HL7_DATE_YEAR_FORMAT = "yyyy";
    private static final DateTimeFormatter HL7_SECONDS_COMPUTER_READABLE = DateTimeFormatter.ofPattern(HL7_DATETIME_FORMAT);
    private static final DateTimeFormatter HL7_SECONDS_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FORMAT_TWO_DIGITS = "%02d";
    public static final int MONTH_PRECISION = 6;
    public static final int DAY_PRECISION = 8;
    public static final int YEAR_PRECISION = 4;

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
                    + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getMonth() + MONTH_PADDING)
                    + "-" + String.format(FORMAT_TWO_DIGITS, baseDateTimeType.getDay());
            default:
                return baseDateTimeType.toCalendar().toInstant().atZone(UK_ZONE_ID)
                    .format(HL7_SECONDS_HUMAN_READABLE);
        }
    }

    public static DateTimeType toDateTypeTime(String effectiveTimeHl7Format) {
        SimpleDateFormat formatter;
        switch (effectiveTimeHl7Format.length()) {
            case YEAR_PRECISION:
                formatter = new SimpleDateFormat(HL7_DATE_YEAR_FORMAT, Locale.ENGLISH);
                break;
            case MONTH_PRECISION:
                formatter = new SimpleDateFormat(HL7_DATE_MONTH_FORMAT, Locale.ENGLISH);
                break;
            case DAY_PRECISION:
                formatter = new SimpleDateFormat(HL7_DATE_FORMAT, Locale.ENGLISH);
                break;
            default:
                formatter = new SimpleDateFormat(HL7_DATETIME_FORMAT, Locale.ENGLISH);
        }

        formatter.setTimeZone(TimeZone.getTimeZone(UK_ZONE_ID));

        Date date = null;
        try {
            date = formatter.parse(effectiveTimeHl7Format);
        } catch (ParseException e) {
            throw new EhrMapperException("Unable to parse Hl7 date to Fhir date format: " + effectiveTimeHl7Format);
        }

        return new DateTimeType(date, TemporalPrecisionEnum.SECOND, TimeZone.getTimeZone(UK_ZONE_ID));
    }
}
