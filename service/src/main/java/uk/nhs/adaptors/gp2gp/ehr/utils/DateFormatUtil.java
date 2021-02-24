package uk.nhs.adaptors.gp2gp.ehr.utils;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.DAY;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MINUTE;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MONTH;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.YEAR;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;

import com.google.common.collect.ImmutableMap;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {
    private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");
    private static final String COULD_NOT_FORMAT_DATE = "Could not format date";

    private static final DateTimeFormatter HL7_YEAR_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter HL7_MONTH_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter HL7_DAY_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HL7_MINUTES_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter HL7_SECONDS_COMPUTER_READABLE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DateTimeFormatter HL7_YEAR_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter HL7_MONTH_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter HL7_DAY_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HL7_MINUTES_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter HL7_SECONDS_HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ImmutableMap<TemporalPrecisionEnum, DateTimeFormatter> HL7_FORMATS_COMPUTER_READABLE = ImmutableMap.of(
        YEAR, HL7_YEAR_COMPUTER_READABLE,
        MONTH, HL7_MONTH_COMPUTER_READABLE,
        DAY, HL7_DAY_COMPUTER_READABLE,
        MINUTE, HL7_MINUTES_COMPUTER_READABLE
    );

    private static final ImmutableMap<TemporalPrecisionEnum, DateTimeFormatter> HL7_FORMATS_HUMAN_READABLE = ImmutableMap.of(
        YEAR, HL7_YEAR_HUMAN_READABLE,
        MONTH, HL7_MONTH_HUMAN_READABLE,
        DAY, HL7_DAY_HUMAN_READABLE,
        MINUTE, HL7_MINUTES_HUMAN_READABLE
    );

    public static String toTextFormat(BaseDateTimeType baseDateTimeType) {
        if (!baseDateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision2(baseDateTimeType.getPrecision(), baseDateTimeType.toCalendar());
    }

    public static String toHl7Format(Instant instant) {
        return instant.atZone(UK_ZONE_ID).format(HL7_SECONDS_COMPUTER_READABLE);
    }

    public static String toHl7Format(BaseDateTimeType baseDateTimeType) {
        if (!baseDateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(baseDateTimeType.getPrecision(), baseDateTimeType.toCalendar());
    }

    private static DateTimeFormatter getFormatStringForPrecision(TemporalPrecisionEnum precisionEnum) {
        return HL7_FORMATS_COMPUTER_READABLE.getOrDefault(precisionEnum, HL7_SECONDS_COMPUTER_READABLE);
    }

    private static String convertWithPrecision(TemporalPrecisionEnum precisionEnum, Calendar calendar) {
        return calendar.toInstant().atZone(UK_ZONE_ID)
            .format(getFormatStringForPrecision(precisionEnum));
    }




    private static DateTimeFormatter getFormatStringForPrecision2(TemporalPrecisionEnum precisionEnum) {
        return HL7_FORMATS_HUMAN_READABLE.getOrDefault(precisionEnum, HL7_SECONDS_HUMAN_READABLE);
    }

    private static String convertWithPrecision2(TemporalPrecisionEnum precisionEnum, Calendar calendar) {
        return calendar.toInstant().atZone(UK_ZONE_ID)
            .format(getFormatStringForPrecision2(precisionEnum));
    }
}
