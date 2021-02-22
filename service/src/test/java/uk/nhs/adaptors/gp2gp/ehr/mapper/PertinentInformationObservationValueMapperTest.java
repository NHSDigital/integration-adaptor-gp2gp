package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public class PertinentInformationObservationValueMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/observation/pertinent/";
    private static final String INPUT_JSON_WITH_CODEABLE_CONCEPT = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-1.json";
    private static final String EXPECTED_CODEABLE_CONCEPT_VALUE = "Code Value: test-code Test display ";
    private static final String INPUT_JSON_WITH_BOOLEAN = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-2.json";
    private static final String EXPECTED_BOOLEAN = "Boolean Value: true ";
    private static final String INPUT_JSON_WITH_RANGE = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-3.json";
    private static final String EXPECTED_RANGE = "Range Value: Low 10.1 test-unit High 20.5 test-unit ";
    private static final String INPUT_JSON_WITH_RATIO = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-4.json";
    private static final String EXPECTED_RATIO = "Ratio Value: < 10 test-unit / > 20 test-unit ";
    private static final String INPUT_JSON_WITH_TIME = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-5.json";
    private static final String EXPECTED_TIME = "Time Value: 12:30:10 ";
    private static final String INPUT_JSON_WITH_DATE_TIME = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-6.json";
    private static final String EXPECTED_DATE_TIME = "DateTime Value: Wed Mar 01 13:50:12 CET 2017 ";
    private static final String INPUT_JSON_WITH_PERIOD = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-pert-info-7.json";
    private static final String EXPECTED_PERIOD = "Period Value: Start Wed Mar 01 13:50:12 CET 2017 End Sun Jun 03 15:12:10 CEST 2018 ";
    private static final PertinentInformationObservationValueMapper PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER =
        new PertinentInformationObservationValueMapper();

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedObservationValueJson_Expect_CorrectXmlOutput(String input, String output) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        boolean isProperValue = PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.isPertinentInformation(observation.getValue());
        assertThat(isProperValue).isTrue();

        String outputMessage = PERTINENT_INFORMATION_OBSERVATION_VALUE_MAPPER.mapObservationValueToPertinentInformation(observation.getValue());
        assertThat(outputMessage).isEqualTo(output);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_CODEABLE_CONCEPT, EXPECTED_CODEABLE_CONCEPT_VALUE),
            Arguments.of(INPUT_JSON_WITH_BOOLEAN, EXPECTED_BOOLEAN),
            Arguments.of(INPUT_JSON_WITH_RANGE, EXPECTED_RANGE),
            Arguments.of(INPUT_JSON_WITH_RATIO, EXPECTED_RATIO),
            Arguments.of(INPUT_JSON_WITH_TIME, EXPECTED_TIME),
            Arguments.of(INPUT_JSON_WITH_DATE_TIME, EXPECTED_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_PERIOD, EXPECTED_PERIOD)
        );
    }
}
