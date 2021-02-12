package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.Stream;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BundleToEhrMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/resourceList/";
    private static final String INPUT_JSON_WITH_ALL_ENCOUNTERS = TEST_FILES_DIRECTORY
        + "example-bundle-1.json";
    private static final String OUTPUT_XML_WITH_ALL_ENCOUNTERS = TEST_FILES_DIRECTORY
        + "expected-output-ehr-1.xml";
    private static final String INPUT_JSON_WITH_NO_REFERENCES_ENCOUNTERS = TEST_FILES_DIRECTORY
        + "example-bundle-2.json";
    private static final String OUTPUT_XML_WITH_NO_REFERENCES_ENCOUNTERS = TEST_FILES_DIRECTORY
        + "expected-output-ehr-2.xml";
    private static final String TEST_ID = "test-id";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private BundleToEhrMapper bundleToEhrMapper;

    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @AfterAll
    public static void deinitialize() {
        TimeZone.setDefault(null);
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        var messageContext = new MessageContext(randomIdGeneratorService);
        var encounterMapper = new EncounterMapper(messageContext);
        bundleToEhrMapper = new BundleToEhrMapper(encounterMapper);
    }

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedBundleJson_Expect_EhrXmlOutput(String input, String output) throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        var parsedBundle = new FhirParseService().parseResource(jsonInput, Bundle.class);

        String outputMessage = bundleToEhrMapper.mapBundleToEhr(parsedBundle);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(Arguments.of(INPUT_JSON_WITH_ALL_ENCOUNTERS, OUTPUT_XML_WITH_ALL_ENCOUNTERS),
            Arguments.of(INPUT_JSON_WITH_NO_REFERENCES_ENCOUNTERS, OUTPUT_XML_WITH_NO_REFERENCES_ENCOUNTERS)
        );
    }
}
