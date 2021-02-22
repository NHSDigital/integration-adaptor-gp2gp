package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatInstantTypeComputerReadable;

import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

@ExtendWith(MockitoExtension.class)
public class DateFormatterUtilTest {
    private static final FhirParseService fhirParser = new FhirParseService();
    private static final String templateObservationWithIssuedInstant = "{\"resourceType\": \"Observation\", \"issued\": \"%s\"}";
    private static final String templateObservationWithValueDate = "{\"resourceType\": \"Observation\", \"valueDate\": \"%s\"}";
    private static final String templateObservationWithValueDateTime = "{\"resourceType\": \"Observation\", \"valueDateTime\": \"%s\"}";



    @ParameterizedTest
    @MethodSource("instantParams")
    public void When_FormattingInstantToHl7_Then_OutputUkZoneAtAvailablePrecision(String input, String expected) {
        String observationJson = String.format(templateObservationWithIssuedInstant, input);
        Observation observation = fhirParser.parseResource(observationJson, Observation.class);

        String actual = formatInstantTypeComputerReadable(observation.getIssuedElement());
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> instantParams() {
        return Stream.of(
            Arguments.of("2019-03-28T10:30:00+00:00", "20190328103000"), //GMT
            Arguments.of("2019-07-28T10:30:00+00:00", "20190728113000")); //BST
    }

//    @ParameterizedTest
//    @MethodSource("dateParams")
//    public void When_ConvertingDateTimeType_Expect_ComputerReadbleDate(String observationString, String immunizationString, String expectedResponse) {
//        Observation observation = fhirParser.parseResource(observationString, Observation.class);
//        Immunization immunization = fhirParser.parseResource(immunizationString, Immunization.class);
//
//        var computerReadableDateTimeType = dateTimeTypeToHl7(observation.getValueDateTimeType());
//        var computerReadableDateType = dateTypeToHl7(immunization.getExpirationDateElement());
//        var computerReadableInstantType = instantTypeToHl7(observation.getIssuedElement());
//
//        assertThat(computerReadableDateTimeType).isEqualTo(expectedResponse);
//        assertThat(computerReadableDateType).isEqualTo(expectedResponse);
//        assertThat(computerReadableInstantType).isEqualTo(expectedResponse);
//    }

//    @ParameterizedTest
//    @MethodSource("dateParams")
//    public void When_ConvertingDateType_Expect_ComputerReadbleDate(TemporalPrecisionEnum precision, String expectedResponse) {
//        DateType dateType = new DateType(date);
//        dateType.setPrecision(precision);
//
//        var computerReadableDate = formatDateTypeComputerReadable(dateType);
//
//        assertThat(computerReadableDate).isEqualTo(expectedResponse);
//    }
//
//    @ParameterizedTest
//    @MethodSource("dateParams")
//    public void When_ConvertingInstantType_Expect_ComputerReadbleDate(TemporalPrecisionEnum precision, String expectedResponse) {
//        InstantType dateType = new InstantType(date);
//        dateType.setPrecision(precision);
//
//        var computerReadableDate = formatInstantTypeComputerReadable(dateType);
//
//        assertThat(computerReadableDate).isEqualTo(expectedResponse);
//    }
//
//    @Test
//    public void When_DateTypeHasNonUkTimeZone_Expect_Correct() {
//        DateType dateType = new DateType(date);
//        dateType.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Singapore")));
//        dateType.setPrecision(SECOND);
//
//        var computerReadableDate = formatDateTypeComputerReadable(dateType);
//
//        assertThat(computerReadableDate).isEqualTo(TEST_YEAR + TEST_MONTH + TEST_DAY + TEST_HOUR + TEST_MINUTE + TEST_SECOND);
//    }



//    private static Stream<Arguments> dateParams() {
//        return Stream.of(
//            Arguments.of(format(obsInstantDateTimeTemplate, "2019", "2019"),
//                format(immDateTypeTemplate, "2019"), "2019"),
//            Arguments.of(format(obsInstantDateTimeTemplate, "2020-03", "2020-03"),
//                format(immDateTypeTemplate, "2020-07"), "202003"),
//            Arguments.of(format(obsInstantDateTimeTemplate, "2017-02-28", "2017-02-28"),
//                format(immDateTypeTemplate, "2017-02-28"), "20170228"),
//            Arguments.of(format(obsInstantDateTimeTemplate, "2019-01-28T10:30", "2019-01-28T10:30"),
//                format(immDateTypeTemplate, "2019-01-28T10:30"), "201901281030"),
//            Arguments.of(format(obsInstantDateTimeTemplate, "2010-07-28T10:30:00", "2019-07-28T10:30:00"),
//                format(immDateTypeTemplate, "2019-07-28T10:30:00"), "20190728103000"),
//            Arguments.of(format(obsInstantDateTimeTemplate, "2019-03-28T10:30:00+00:00", "2019-03-28T10:30:00+00:00"),
//                format(immDateTypeTemplate, "2019-07-28T10:30:00+01:00"), "20190728103000")
//        );
//    }
}
