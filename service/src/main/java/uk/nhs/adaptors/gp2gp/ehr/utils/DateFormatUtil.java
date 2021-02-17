package uk.nhs.adaptors.gp2gp.ehr.utils;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.DAY;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MINUTE;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MONTH;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.YEAR;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.InstantType;

import com.google.common.collect.ImmutableMap;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {

    public static final String UK_TIMEZONE = "Europe/London";
    public static final String COULD_NOT_FORMAT_DATE = "Could not format date";
    private static final ImmutableMap<TemporalPrecisionEnum, String> YEAR_PATTERN_MAP = ImmutableMap.of(
        YEAR, "yyyy",
        MONTH, "yyyyMM",
        DAY, "yyyyMMdd",
        MINUTE, "yyyyMMddHHmm"
    );

    public static String formatDateTimeType(DateTimeType dateTimeType) {
        if (!dateTimeType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        return Optional.ofNullable(YEAR_PATTERN_MAP.get(dateTimeType.getPrecision()))
            .map(pattern -> localDateTime(dateTimeType, pattern))
            .orElseGet(() -> localDateTime(dateTimeType, "yyyyMMddHHmmss"));
    }

    private static String localDateTime(DateTimeType effectiveDateTimeType, String pattern) {
        return LocalDateTime.ofInstant(effectiveDateTimeType.toCalendar().toInstant(), ZoneId.of(UK_TIMEZONE))
            .format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatInstantType(InstantType dateInstantType) {
        if (!dateInstantType.hasValue()) {
            throw new EhrMapperException(COULD_NOT_FORMAT_DATE);
        }

        DateTimeType dateTimeType = new DateTimeType(dateInstantType.toCalendar().toInstant().toString());

        return formatDateTimeType(dateTimeType);
    }
}
