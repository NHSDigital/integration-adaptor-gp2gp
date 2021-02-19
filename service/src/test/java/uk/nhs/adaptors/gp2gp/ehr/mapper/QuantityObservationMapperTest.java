package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class QuantityObservationMapperTest {
    private static final QuantityObservationMapper QUANTITY_OBSERVATION_MAPPER = new QuantityObservationMapper();
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/quantity/";

    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_NO_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-1.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_NO_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-1.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-2.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-2.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-3.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-3.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-4.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-4.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-5.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-5.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_NO_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-6.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_NO_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-6.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-7.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-7.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR= TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-8.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-8.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-9.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-9.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR= TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-10.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-10.xml";

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedQuantityJson_Expect_CorrectXmlOutput(String input, String output) throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Observation parseObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = QUANTITY_OBSERVATION_MAPPER.mapObservationValueQuantity(parseObservation.getValueQuantity());
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_NO_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_NO_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_NO_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_NO_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_LESS_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR)

        );
    }
}
