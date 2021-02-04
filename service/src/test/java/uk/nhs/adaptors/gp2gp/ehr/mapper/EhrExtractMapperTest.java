package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class EhrExtractMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;
    private static final String JSON_INPUT_FILE = "gpc-access-structured.json";
    private static final String JSON_INPUT_FILE_WITH_NO_PATIENT = "gpc-access-structured-with-no-patient.json";
    private static final String EXPECTED_XML_TO_JSON_FILE = "ExpectedEhrExtractResponseFromJson.xml";
    private static final String TEST_ID = "test-id";
    private static final String TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String TEST_REQUEST_ID = "test-request-id";
    private static final String TEST_NHS_NUMBER = "1234567890";
    private static final String TEST_FROM_ODS_CODE = "test-from-ods-code";
    private static final String TEST_TO_ODS_CODE = "test-to-ods-code";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";
    private static final String JSON_WITH_NO_CONTENT = "{}";
    private static final String EXPECTED_NO_CONTENT_EXCEPTION_MESSAGE =
        "Failed to parse JSON encoded FHIR content: Did not find any content to parse";
    private static final String EXPECTED_NO_RESOURCE_TYPE_EXCEPTION_MESSAGE =
        "Invalid JSON content detected, missing required element: 'resourceType'";
    private static final String EXPECTED_NO_PATIENT_EXCEPTION_MESSAGE = "Missing patient resource in Fhir Bundle.";

    private static String inputJsonFileContent;
    private static String inputJsonFileWithNoPatientContent;
    private static String expectedJsonToXmlContent;
    private static GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;
    @Mock
    private MapperService mapperService;

    private EhrExtractMapper ehrExtractMapper;

    @BeforeAll
    public static void initialize() throws IOException {
        inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + JSON_INPUT_FILE);
        inputJsonFileWithNoPatientContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + JSON_INPUT_FILE_WITH_NO_PATIENT);
        expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_XML_TO_JSON_FILE);

        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(TEST_NHS_NUMBER)
            .conversationId(TEST_CONVERSATION_ID)
            .requestId(TEST_REQUEST_ID)
            .fromOdsCode(TEST_FROM_ODS_CODE)
            .toOdsCode(TEST_TO_ODS_CODE)
            .build();
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        ehrExtractMapper = new EhrExtractMapper(new FhirParseService(), randomIdGeneratorService, timestampService, mapperService);
    }

    @Test
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput() {
        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapJsonToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            inputJsonFileContent);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        assertThat(output).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);

        verify(mapperService).mapToHl7(any(Bundle.class));
    }

    @ParameterizedTest
    @MethodSource("exceptionParams")
    public void When_MappingInvalidJsonRequestBody_Expect_FhirValidationExceptionThrown(String jsonContent, String expectedMessage) {
        Exception exception = assertThrows(FhirValidationException.class,
            () -> ehrExtractMapper.mapJsonToEhrFhirExtractParams(getGpcStructuredTaskDefinition,
                jsonContent));
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);

        verify(mapperService, never()).mapToHl7(any());
    }

    private static Stream<Arguments> exceptionParams() {
        return Stream.of(
            Arguments.of(StringUtils.EMPTY, EXPECTED_NO_CONTENT_EXCEPTION_MESSAGE),
            Arguments.of(JSON_WITH_NO_CONTENT, EXPECTED_NO_RESOURCE_TYPE_EXCEPTION_MESSAGE),
            Arguments.of(inputJsonFileWithNoPatientContent, EXPECTED_NO_PATIENT_EXCEPTION_MESSAGE)
        );
    }
}
