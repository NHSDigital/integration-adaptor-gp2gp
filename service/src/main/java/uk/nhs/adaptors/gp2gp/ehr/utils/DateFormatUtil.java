package uk.nhs.adaptors.gp2gp.ehr.utils;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.DAY;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MINUTE;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MONTH;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.YEAR;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

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
    private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");
    private static final String COULD_NOT_FORMAT_DATE = "Could not format date";

    private static final DateTimeFormatter HL7_YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter HL7_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter HL7_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HL7_MINUTES = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter HL7_SECONDS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final ImmutableMap<TemporalPrecisionEnum, DateTimeFormatter> HL7_FORMATS = ImmutableMap.of(
        YEAR, HL7_YEAR,
        MONTH, HL7_MONTH,
        DAY, HL7_DAY,
        MINUTE, HL7_MINUTES
    );

    //TODO: NIAD-1082
    private static final String TEXT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter TEXT_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern(TEXT_DATE_TIME)
        .toFormatter();

    // FIXME: NIAD-1082 using Date object will incorrectly convert into system local time
    @Deprecated(forRemoval = true)
    public static Object toTextFormat(Date date) {
        if (date == null) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return date.toInstant()
            .atZone(UK_ZONE_ID)
            .format(TEXT_DATE_TIME_FORMATTER);
    }

    public static String toHl7Format(Instant instant) {
        return instant.atZone(UK_ZONE_ID).format(HL7_SECONDS);
    }

    public static String toHl7Format(DateType dateType) {
        if (!dateType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(dateType.getPrecision(), dateType.toCalendar());
    }

    public static String toHl7Format(InstantType dateInstantType) {
        if (!dateInstantType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(dateInstantType.getPrecision(), dateInstantType.toCalendar());
    }

    public static String toHl7Format(DateTimeType dateTimeType) {
        if (!dateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return convertWithPrecision(dateTimeType.getPrecision(), dateTimeType.toCalendar());
    }

    private static DateTimeFormatter getFormatStringForPrecision(TemporalPrecisionEnum precisionEnum) {
        return HL7_FORMATS.getOrDefault(precisionEnum, HL7_SECONDS);
    }

    private static String convertWithPrecision(TemporalPrecisionEnum precisionEnum, Calendar calendar) {
        return calendar.toInstant().atZone(UK_ZONE_ID)
            .format(getFormatStringForPrecision(precisionEnum));
    }
}
