package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.DAY;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MILLI;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MINUTE;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.MONTH;
import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.SECOND;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDateTimeTypeComputerReadable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.YEAR;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDateTypeComputerReadable;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatInstantType;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

@ExtendWith(MockitoExtension.class)
public class DateFormatterUtil {
    private static final String TEST_YEAR = "2021";
    private static final String TEST_MONTH = "06";
    private static final String TEST_DAY = "20";
    private static final String TEST_HOUR = "13";
    private static final String TEST_MINUTE = "10";
    private static final String TEST_SECOND = "25";

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static Date date;

    @BeforeAll
    public static void setup() throws ParseException {
        date = FORMAT.parse(TEST_YEAR + "/" + TEST_MONTH + "/" + TEST_DAY
            + " " + TEST_HOUR + ":" + TEST_MINUTE + ":" + TEST_SECOND);
    }

    @ParameterizedTest
    @MethodSource("dateParams")
    public void When_ConvertingDateTimeType_Expect_ComputerReadbleDate(TemporalPrecisionEnum precision, String expectedResponse) {
        DateTimeType dateTimeType = new DateTimeType(date);
        dateTimeType.setPrecision(precision);

        var computerReadableDate = formatDateTimeTypeComputerReadable(dateTimeType);

        assertThat(computerReadableDate).isEqualTo(expectedResponse);
    }

    @ParameterizedTest
    @MethodSource("dateParams")
    public void When_ConvertingDateType_Expect_ComputerReadbleDate(TemporalPrecisionEnum precision, String expectedResponse) {
        DateType dateType = new DateType(date);
        dateType.setPrecision(precision);

        var computerReadableDate = formatDateTypeComputerReadable(dateType);

        assertThat(computerReadableDate).isEqualTo(expectedResponse);
    }

    @ParameterizedTest
    @MethodSource("dateParams")
    public void When_ConvertingInstantType_Expect_ComputerReadbleDate(TemporalPrecisionEnum precision, String expectedResponse) {
        InstantType dateType = new InstantType(date);
        dateType.setPrecision(precision);

        var computerReadableDate = formatInstantType(dateType);

        assertThat(computerReadableDate).isEqualTo(expectedResponse);
    }

    private static Stream<Arguments> dateParams() {
        return Stream.of(
            Arguments.of(YEAR, TEST_YEAR),
            Arguments.of(MONTH, TEST_YEAR + TEST_MONTH),
            Arguments.of(DAY, TEST_YEAR + TEST_MONTH + TEST_DAY),
            Arguments.of(MINUTE, TEST_YEAR + TEST_MONTH + TEST_DAY + TEST_HOUR + TEST_MINUTE),
            Arguments.of(SECOND, TEST_YEAR + TEST_MONTH + TEST_DAY + TEST_HOUR + TEST_MINUTE + TEST_SECOND),
            Arguments.of(MILLI, TEST_YEAR + TEST_MONTH + TEST_DAY + TEST_HOUR + TEST_MINUTE + TEST_SECOND)
        );
    }
}
