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

public class XmlObservationValueMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/observation/value/";
    private static final String INPUT_JSON_WITH_STRING_TYPE = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-value-1.json";
    private static final String OUTPUT_XML_WITH_STRING_TYPE = TEST_FILES_DIRECTORY
        + "example-output-observation-value-1.xml";
    private static final XmlObservationValueMapper XML_OBSERVATION_VALUE_MAPPER = new XmlObservationValueMapper();

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedObservationValueJson_Expect_CorrectXmlOutput(String input, String output) throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        boolean isProperValue = XML_OBSERVATION_VALUE_MAPPER.isXmlValueType(observation.getValue());
        assertThat(isProperValue).isTrue();

        String outputMessage = XML_OBSERVATION_VALUE_MAPPER.mapObservationValueToXmlElement(observation.getValue());
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_STRING_TYPE, OUTPUT_XML_WITH_STRING_TYPE)
        );
    }
}
