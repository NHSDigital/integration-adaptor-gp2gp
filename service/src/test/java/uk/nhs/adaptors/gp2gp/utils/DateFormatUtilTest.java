package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

@ExtendWith(MockitoExtension.class)
public class DateFormatUtilTest {
    private static final FhirParseService FHIR_PARSER = new FhirParseService();
    private static final String INSTANT_OBSERVATION_TEMPLATE = "{\"resourceType\": \"Observation\", \"issued\": \"%s\"}";
    private static final String DATETYPE_IMMUNIZATION_TEMPLATE = "{\"resourceType\": \"Immunization\", \"expirationDate\": \"%s\"}";
    private static final String DATETIME_OBSERVATION_TEMPLATE = "{\"resourceType\": \"Observation\", \"valueDateTime\": \"%s\"}";

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
}
