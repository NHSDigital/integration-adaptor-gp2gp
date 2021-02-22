package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import java.util.stream.Stream;

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
    private static final String DATE_OBSERVATION_TEMPLATE = "{\"resourceType\": \"Observation\", \"valueDate\": \"%s\"}";
    private static final String DATETIME_OBSERVATION_TEMPLATE = "{\"resourceType\": \"Observation\", \"valueDateTime\": \"%s\"}";

    @ParameterizedTest
    @MethodSource("instantParams")
    public void When_FormattingInstantToHl7_Expect_Hl7InUkZone(String input, String expected) {
        String observationJson = String.format(INSTANT_OBSERVATION_TEMPLATE, input);
        Observation observation = FHIR_PARSER.parseResource(observationJson, Observation.class);

        String actual = toHl7Format(observation.getIssuedElement());
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> instantParams() {
        return Stream.of(
            Arguments.of("2019-03-28T10:30:05+00:00", "20190328103005"), // GMT
            Arguments.of("2019-07-28T10:30:05+00:00", "20190728113005"), // BST
            Arguments.of("2019-07-28T23:30:05+00:00", "20190729003005"), // BST - over midnight
            Arguments.of("2019-07-28T10:30:00+05:00", "20190728063000")); // other offset
    }

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
