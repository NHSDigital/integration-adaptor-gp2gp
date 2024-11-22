package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.stream.Stream;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public class PertinentInformationObservationValueMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/observation/pertinent/";
    private static final String INPUT_JSON_WITH_CODEABLE_CONCEPT = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-1.json";
    private static final String INPUT_JSON_WITH_BOOLEAN = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-2.json";
    private static final String INPUT_JSON_WITH_RANGE = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-3.json";
    private static final String INPUT_JSON_WITH_RATIO = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-4.json";
    private static final String INPUT_JSON_WITH_TIME = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-5.json";
    private static final String INPUT_JSON_WITH_DATE_TIME = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-6.json";
    private static final String INPUT_JSON_WITH_PERIOD = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-7.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-8.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE_WITH_LOW = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-9.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE_WITH_HIGH = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-10.json";
    private static final String INPUT_JSON_WITH_RATIO_WITH_EMPTY_QUANTITIES = TEST_FILES_DIRECTORY
            + "example-observation-resource-with-pert-info-11.json";
    private static final String INPUT_JSON_WITH_INVALID_VALUE = TEST_FILES_DIRECTORY
        + "invalid-value-observation-resource.json";
    private static final String EXPECTED_CODEABLE_CONCEPT_VALUE = "Code Value: test-code Test display ";
    private static final String EXPECTED_BOOLEAN = "Boolean Value: true ";
    private static final String EXPECTED_RANGE = "Range Value: Low 10.1 test-unit High 20.5 test-unit ";
    private static final String EXPECTED_RATIO = "Ratio Value: < 10 test-unit / > 20 test-unit ";
    private static final String EXPECTED_RATIO_WITH_EMPTY_QUANTITIES = "Ratio Value:  /  ";
    private static final String EXPECTED_TIME = "Time Value: 12:30:10 ";
    private static final String EXPECTED_DATE_TIME = "DateTime Value: 2017-03-01 12:50:12 ";
    private static final String EXPECTED_PERIOD = "Period Value: Start 2017-03-01 12:50:12 End 2018-06-03 14:12:10 ";
    private static final String EXPECTED_REFERENCE_RANGE = "Range: Text: Test reference range text Low: 10 High: 20 ";
    private static final String EXPECTED_REFERENCE_RANGE_WITH_LOW = "Range: Text: Test reference range text Low: 10 ";
    private static final String EXPECTED_REFERENCE_RANGE_WITH_HIGH = "Range: Text: Test reference range text High: 20 ";
    private static final PertinentInformationObservationValueMapper PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER =
        new PertinentInformationObservationValueMapper();
    private FhirContext fhirCtx = FhirContext.forDstu3();

    @ParameterizedTest
    @MethodSource("testValueFilePaths")
    public void When_MappingParsedObservationValueJson_Expect_CorrectXmlOutput(String input, String output) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Observation observation = new FhirParseService(fhirCtx).parseResource(jsonInput, Observation.class);

        boolean isProperValue =
            PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.isPertinentInformation(observation.getValue());
        assertThat(isProperValue).isTrue();

        String outputMessage =
            PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.mapObservationValueToPertinentInformation(observation.getValue());
        assertThat(outputMessage).isEqualTo(output);
    }

    @ParameterizedTest
    @MethodSource("testReferenceRangeFilePaths")
    public void When_MappingParsedObservationJsonWithReferenceRange_Expect_CorrectXmlOutput(String input,
            String output) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Observation observation = new FhirParseService(fhirCtx).parseResource(jsonInput, Observation.class);

        String outputMessage =
            PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.mapReferenceRangeToPertinentInformation(observation.getReferenceRangeFirstRep());
        assertThat(outputMessage).isEqualTo(output);
    }

    @Test
    public void When_MappingParsedObservationInvalidValueJson_Expect_IllegalArgumentException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_INVALID_VALUE);
        Observation observation = new FhirParseService(fhirCtx).parseResource(jsonInput, Observation.class);

        boolean isProperValue =
            PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.isPertinentInformation(observation.getValue());
        assertThat(isProperValue).isFalse();

        assertThrows(IllegalArgumentException.class, ()
            -> PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.mapObservationValueToPertinentInformation(observation.getValue()));
    }

    private static Stream<Arguments> testValueFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_CODEABLE_CONCEPT, EXPECTED_CODEABLE_CONCEPT_VALUE),
            Arguments.of(INPUT_JSON_WITH_BOOLEAN, EXPECTED_BOOLEAN),
            Arguments.of(INPUT_JSON_WITH_RANGE, EXPECTED_RANGE),
            Arguments.of(INPUT_JSON_WITH_RATIO, EXPECTED_RATIO),
            Arguments.of(INPUT_JSON_WITH_TIME, EXPECTED_TIME),
            Arguments.of(INPUT_JSON_WITH_DATE_TIME, EXPECTED_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_PERIOD, EXPECTED_PERIOD),
            Arguments.of(INPUT_JSON_WITH_RATIO_WITH_EMPTY_QUANTITIES, EXPECTED_RATIO_WITH_EMPTY_QUANTITIES)
        );
    }

    private static Stream<Arguments> testReferenceRangeFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE, EXPECTED_REFERENCE_RANGE),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE_WITH_LOW, EXPECTED_REFERENCE_RANGE_WITH_LOW),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE_WITH_HIGH, EXPECTED_REFERENCE_RANGE_WITH_HIGH)
        );
    }
}
