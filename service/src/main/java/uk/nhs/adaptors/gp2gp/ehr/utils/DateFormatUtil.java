package uk.nhs.adaptors.gp2gp.ehr.utils;

import lombok.SneakyThrows;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.InstantType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class DateFormatUtil {

    @SneakyThrows
    public static String formatDateTimeType(DateTimeType dateTimeType) {
        if (dateTimeType == null) {
            throw new EhrMapperException("Could not format date");
        }

        switch (dateTimeType.getPrecision()) {
            case YEAR:
                return localDateTime(dateTimeType, "yyyy");
            case MONTH:
                return localDateTime(dateTimeType, "yyyyMM");
            case DAY:
                return localDateTime(dateTimeType, "yyyyMMdd");
            default:
                return localDateTime(dateTimeType, "yyyyMMddHHmmss");
        }
    }

    private static String localDateTime(DateTimeType effectiveDateTimeType, String pattern) {
        return LocalDateTime.ofInstant(effectiveDateTimeType.toCalendar().toInstant(), ZoneId.of("Europe/London"))
            .format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatInstantType(InstantType dateInstantType) {
        if (!dateInstantType.hasValue()) {
            throw new EhrMapperException("Could not format date");
        }

        DateTimeType dateTimeType = new DateTimeType(dateInstantType.toCalendar().toInstant().toString());

        return formatDateTimeType(dateTimeType);
    }
}
