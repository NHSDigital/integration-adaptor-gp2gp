package uk.nhs.adaptors.gp2gp.utils;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

import static ca.uhn.fhir.model.api.TemporalPrecisionEnum.SECOND;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDateTimeTypeComputerReadable;

import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDateTypeComputerReadable;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatInstantTypeComputerReadable;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

@ExtendWith(MockitoExtension.class)
public class DateFormatterUtilTest {
    private static final FhirParseService fhirParser = new FhirParseService();
    private static final String obsInstantDateTimeTemplate = "{\"resourceType\": \"Observation\", \"issued\": \"%s\", \"valueDateTime\": \"%s\"}";
    private static final String immDateTypeTemplate = "{\"resourceType\": \"Immunization\", \"expirationDate\": \"%s\"}";

    @ParameterizedTest
    @MethodSource("dateParams")
    public void When_ConvertingDateTimeType_Expect_ComputerReadbleDate(String observationString, String immunizationString, String expectedResponse) {
        Observation observation = fhirParser.parseResource(observationString, Observation.class);
        Immunization immunization = fhirParser.parseResource(immunizationString, Immunization.class);

        var computerReadableDateTimeType = formatDateTimeTypeComputerReadable(observation.getValueDateTimeType());
        var computerReadableDateType = formatDateTypeComputerReadable(immunization.getExpirationDateElement());
        var computerReadableInstantType = formatInstantTypeComputerReadable(observation.getIssuedElement());

        assertThat(computerReadableDateTimeType).isEqualTo(expectedResponse);
        assertThat(computerReadableDateType).isEqualTo(expectedResponse);
        assertThat(computerReadableInstantType).isEqualTo(expectedResponse);
    }

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



    private static Stream<Arguments> dateParams() {
        return Stream.of(
            Arguments.of(format(obsInstantDateTimeTemplate, "2019", "2019"),
                format(immDateTypeTemplate, "2019"), "2019"),
            Arguments.of(format(obsInstantDateTimeTemplate, "2020-03", "2020-03"),
                format(immDateTypeTemplate, "2020-07"), "202003"),
            Arguments.of(format(obsInstantDateTimeTemplate, "2017-02-28", "2017-02-28"),
                format(immDateTypeTemplate, "2017-02-28"), "20170228"),
            Arguments.of(format(obsInstantDateTimeTemplate, "2019-01-28T10:30", "2019-01-28T10:30"),
                format(immDateTypeTemplate, "2019-01-28T10:30"), "201901281030"),
            Arguments.of(format(obsInstantDateTimeTemplate, "2010-07-28T10:30:00", "2019-07-28T10:30:00"),
                format(immDateTypeTemplate, "2019-07-28T10:30:00"), "20190728103000"),
            Arguments.of(format(obsInstantDateTimeTemplate, "2019-03-28T10:30:00+00:00", "2019-03-28T10:30:00+00:00"),
                format(immDateTypeTemplate, "2019-07-28T10:30:00+01:00"), "20190728103000")
        );
    }
}
