package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toDateTypeTime;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toTextFormat;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toTextFormatStraight;

import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Specimen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

@ExtendWith(MockitoExtension.class)
public class DateFormatUtilTest {
    private static final FhirParseService FHIR_PARSER = new FhirParseService();
    private static final String INSTANT_OBSERVATION_TEMPLATE = "{\"resourceType\": \"Observation\", \"issued\": \"%s\"}";
    private static final String DATETYPE_IMMUNIZATION_TEMPLATE = "{\"resourceType\": \"Immunization\", \"expirationDate\": \"%s\"}";
    private static final String DATETIME_OBSERVATION_TEMPLATE = "{\"resourceType\": \"Observation\", \"valueDateTime\": \"%s\"}";
    private static final String DATETIME_SPECIMEN_TEMPLATE = "{\"resourceType\": \"Specimen\", \"receivedTime\": \"%s\"}";

    @ParameterizedTest
    @MethodSource("instantParams")
    public void When_FormattingInstantToHl7_Expect_Hl7InUkZone(String input, String expected) {
        String observationJson = String.format(INSTANT_OBSERVATION_TEMPLATE, input);
        Observation observation = FHIR_PARSER.parseResource(observationJson, Observation.class);

        String actual = toHl7Format(observation.getIssuedElement());
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("dateParams")
    public void When_FormattingDateTypeToHl7_Expect_Hl7InUkZone(String input, String expected) {
        String observationJson = String.format(DATETYPE_IMMUNIZATION_TEMPLATE, input);
        Immunization immunization = FHIR_PARSER.parseResource(observationJson, Immunization.class);

        String actual = toHl7Format(immunization.getExpirationDateElement());
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("dateParams")
    public void When_FormattingDateTimeTypeToHl7_Expect_Hl7InUkZone(String input, String expected) {
        String observationJson = String.format(DATETIME_OBSERVATION_TEMPLATE, input);
        Observation observation = FHIR_PARSER.parseResource(observationJson, Observation.class);

        String actual = toHl7Format(observation.getValueDateTimeType());
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("dateTextParams")
    public void When_FormattingDateTimeTypeToText_Expect_Hl7InUkZone(String input, String expected) {
        String observationJson = String.format(DATETIME_OBSERVATION_TEMPLATE, input);
        Observation observation = FHIR_PARSER.parseResource(observationJson, Observation.class);

        String actual = toTextFormat(observation.getValueDateTimeType());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void When_FormattingDateToText_Expect_StringWithDateHoursMinutes() {
        final String expected = "2005-05-02 21:37";
        String specimenJson = String.format(DATETIME_SPECIMEN_TEMPLATE, "2005-05-02T21:37:05+00:00");
        Specimen specimen = FHIR_PARSER.parseResource(specimenJson, Specimen.class);

        String actual = toTextFormatStraight(specimen.getReceivedTime().toInstant());
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"20190328103005,20190328103005", "1973,19730101000000", "200808,20080801000000", "20190728,20190728000000"})
    public void When_FormattingDateTimeTypeFromHl7FormatTextBackToDate_Expect_Hl7InUkZone(String firstParsedDate,
        String expectedConvertedBackDateTimeTypeString) {
        BaseDateTimeType convertedBackToDateTimeType = toDateTypeTime(firstParsedDate);

        assertThat(toHl7Format(convertedBackToDateTimeType)).isEqualTo(expectedConvertedBackDateTimeTypeString);
    }

    private static Stream<Arguments> instantParams() {
        return Stream.of(
            Arguments.of("2019-03-28T10:30:05+00:00", "20190328103005"),
            Arguments.of("2019-07-28T10:30:05+00:00", "20190728113005"),
            Arguments.of("1740-07-28T23:30:05+00:00", "17400728232850"),
            Arguments.of("1960-07-28T10:30:55+05:00", "19600728063055"));
    }

    private static Stream<Arguments> dateParams() {
        return Stream.of(
            Arguments.of("1973", "1973"),
            Arguments.of("2008-08", "200808"),
            Arguments.of("2019-07-28", "20190728"),
            Arguments.of("2010-02-22T10:30:05+00:00", "20100222103005"),
            Arguments.of("2019-09-28T10:30:05+00:00", "20190928113005"),
            Arguments.of("1740-07-07T23:30:05+00:00", "17400707232850"),
            Arguments.of("1960-06-13T10:30:55+05:00", "19600613063055"));
    }

    private static Stream<Arguments> dateTextParams() {
        return Stream.of(
            Arguments.of("1973", "1973"),
            Arguments.of("2008-08", "2008-08"),
            Arguments.of("2019-07-28", "2019-07-28"),
            Arguments.of("2010-02-22T10:30:05+00:00", "2010-02-22 10:30:05"),
            Arguments.of("2019-09-28T10:30:05+00:00", "2019-09-28 11:30:05"),
            Arguments.of("1740-07-07T23:30:05+00:00", "1740-07-07 23:28:50"),
            Arguments.of("1960-06-13T10:30:55+05:00", "1960-06-13 06:30:55"));
    }
}
