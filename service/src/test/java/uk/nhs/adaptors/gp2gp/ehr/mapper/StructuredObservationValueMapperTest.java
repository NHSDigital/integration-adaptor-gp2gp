package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public class StructuredObservationValueMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/observation/value/";
    private static final String INPUT_JSON_WITH_STRING_TYPE = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-value-1.json";
    private static final String INPUT_JSON_WITH_INVALID_VALUE = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-value-3.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-interpretation-4.json";
    private static final String INPUT_JSON_WITH_REF_RANGE_FULL_DATA = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-reference-range-full-data.json";
    private static final String INPUT_JSON_WITH_REF_RANGE_TEXT_ONLY = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-reference-range-text-only.json";
    private static final String INPUT_JSON_WITH_REF_RANGE_HIGH_ONLY = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-reference-range-high-only.json";
    private static final String INPUT_JSON_WITH_REF_RANGE_LOW_ONLY = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-reference-range-low-only.json";
    private static final String OUTPUT_XML_WITH_STRING_TYPE = TEST_FILES_DIRECTORY
        + "example-output-observation-value-1.xml";
    private static final String OUTPUT_XML_WITH_INTERPRETATION = TEST_FILES_DIRECTORY
        + "example-output-observation-with-interpretation-3.xml";
    private static final String OUTPUT_XML_WITH_REF_RANGE_FULL_DATA = TEST_FILES_DIRECTORY
        + "example-output-observation-with-reference-range-full-data.xml";
    private static final String OUTPUT_XML_WITH_REF_RANGE_TEXT_ONLY = TEST_FILES_DIRECTORY
        + "example-output-observation-with-reference-range-text-only.xml";
    private static final String OUTPUT_XML_WITH_REF_RANGE_HIGH_ONLY = TEST_FILES_DIRECTORY
        + "example-output-observation-with-reference-range-high-only.xml";
    private static final String OUTPUT_XML_WITH_REF_RANGE_LOW_ONLY = TEST_FILES_DIRECTORY
        + "example-output-observation-with-reference-range-low-only.xml";

    private static final StructuredObservationValueMapper XML_OBSERVATION_VALUE_MAPPER = new StructuredObservationValueMapper();

    @Test
    public void When_MappingParsedObservationStringValueJson_Expect_CorrectXmlOutput() throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_STRING_TYPE);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_STRING_TYPE);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        boolean isProperValue = XML_OBSERVATION_VALUE_MAPPER.isStructuredValueType(observation.getValue());
        assertThat(isProperValue).isTrue();

        String outputMessage = XML_OBSERVATION_VALUE_MAPPER.mapObservationValueToStructuredElement(observation.getValue());
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @ParameterizedTest
    @MethodSource("referenceRangeTestParams")
    public void When_MappingParsedObservationReferenceRangeJson_Expect_CorrectXmlOutput(
        String inputJsonPath,
        String expectedOutputXmlPath
    ) throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(expectedOutputXmlPath);

        var jsonInput = ResourceTestFileUtils.getFileContent(inputJsonPath);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = XML_OBSERVATION_VALUE_MAPPER.mapReferenceRangeType(observation.getReferenceRangeFirstRep());
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationInterpretationJson_Expect_CorrectXmlOutput() throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_INTERPRETATION);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_INTERPRETATION);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = XML_OBSERVATION_VALUE_MAPPER.mapInterpretation(observation.getInterpretation()
            .getCodingFirstRep());
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationInvalidValueJson_Expect_IllegalArgumentException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_INVALID_VALUE);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        boolean isProperValue =
            XML_OBSERVATION_VALUE_MAPPER.isStructuredValueType(observation.getValue());
        assertThat(isProperValue).isFalse();

        assertThrows(IllegalArgumentException.class, ()
            -> XML_OBSERVATION_VALUE_MAPPER.mapObservationValueToStructuredElement(observation.getValue()));
    }

    private static Stream<Arguments> referenceRangeTestParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_REF_RANGE_FULL_DATA, OUTPUT_XML_WITH_REF_RANGE_FULL_DATA),
            Arguments.of(INPUT_JSON_WITH_REF_RANGE_HIGH_ONLY, OUTPUT_XML_WITH_REF_RANGE_HIGH_ONLY),
            Arguments.of(INPUT_JSON_WITH_REF_RANGE_LOW_ONLY, OUTPUT_XML_WITH_REF_RANGE_LOW_ONLY),
            Arguments.of(INPUT_JSON_WITH_REF_RANGE_TEXT_ONLY, OUTPUT_XML_WITH_REF_RANGE_TEXT_ONLY)
        );
    }
}
