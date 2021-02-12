package uk.nhs.adaptors.gp2gp.ehr.utils;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.DateTimeType;

public class DateFormatUtil {
    private static final int ZERO = 0;
    private static final String UK_ZONE_ID = "Europe/London";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();
    private static final DateTimeFormatter DATE_TIME_FORMATTER_SHORT = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .toFormatter();

    private static final String INPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String OUTPUT_PATTERN = "yyyyMMddHHmmss";
    private static final String FHIR_INPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static String formatDate(Date date) {
        if (date == null) {
            throw new EhrMapperException("Could not format date");
        }

        var patternDate = "-?[0-9]{4}(-(0[1-9]|1[0-2])(-(0[0-9]|[1-2][0-9]|3[0-1]))?)?";
        var patternDateTime = "-?([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T" +
            "([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\\\.[0-9]+)?(Z|(\\\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?";

        var calendarDateTime = date.toInstant().toString();
        if (calendarDateTime.matches(patternDate)){
            return DATE_TIME_FORMATTER_SHORT.format(
                date.toInstant()
                    .atZone(ZoneId.of(UK_ZONE_ID))
                    .toLocalDateTime());
        } else if (calendarDateTime.matches(patternDateTime)) {
            return DATE_TIME_FORMATTER.format(
                date.toInstant()
                    .atZone(ZoneId.of(UK_ZONE_ID))
                    .toLocalDateTime());
        }

        //LocalDateTime.ofInstant(dateTimeType.toCalendar().toInstant(), ZoneId.of("Europe/London")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));


        return null;
    }

    public static String formatPeriod(Period period) {
        Date newDate = new Date();

        if (period.getDays() == 0) {

        }
        return "";
    }



//    public static String formatDateTimeType(DateTimeType dateTimeType){
////        var dateString = dateTimeType.getYear()
////            + dateTimeType.getMonth()
////            + dateTimeType.getDay()
////            + dateTimeType.getHour()
////            + dateTimeType.getMinute()
////            + dateTimeType.getSecond();
//
//        return
//    }

    @SneakyThrows
    public static String formatDateTest(Date date, DateTimeType effectiveDateTimeType) {
        if (date == null) {
            throw new EhrMapperException("Could not format date");
        }

        var patternDate = "-?[0-9]{4}(-(0[1-9]|1[0-2])(-(0[0-9]|[1-2][0-9]|3[0-1]))?)?";
        var patternDateTime = "-?([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T" +
            "([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\\\.[0-9]+)?(Z|(\\\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?";

        var calendarDateTime = date.toInstant().toString();
        var datetime = "null";
        date = new SimpleDateFormat(INPUT_PATTERN).parse(calendarDateTime);

        effectiveDateTimeType.getPrecision();
        effectiveDateTimeType.getTimeZone();
        // hours, minutes, seconds, millisecond equal zero

        if (effectiveDateTimeType.getHour().equals(ZERO)){
            datetime = new SimpleDateFormat("yyyy-MM-dd")
                .format(date);
        } else if (calendarDateTime.matches(patternDateTime)) {
            datetime =  new SimpleDateFormat(OUTPUT_PATTERN)
                .format(date);
        }

        var temp = effectiveDateTimeType.toString().replace("DateTimeType[", StringUtils.EMPTY);
        temp = temp.replace("", StringUtils.EMPTY);

        //new SimpleDateFormat("yyyy-MM-dd").format(calendarDateTime);


        return datetime;
    }

}
