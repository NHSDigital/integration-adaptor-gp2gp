package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterEach;
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

import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EhrExtractMapperTest extends MapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;
    private static final String JSON_INPUT_FILE = "gpc-access-structured.json";
    private static final String EXPECTED_XML_TO_JSON_FILE = "ExpectedEhrExtractResponseFromJson.xml";
    private static final String TEST_ID_1 = "test-id-1";
    private static final String TEST_ID_2 = "test-id-2";
    private static final String TEST_ID_3 = "test-id-3";
    private static final String TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String TEST_REQUEST_ID = "test-request-id";
    private static final String TEST_NHS_NUMBER = "1234567890";
    private static final String TEST_FROM_ODS_CODE = "test-from-ods-code";
    private static final String TEST_TO_ODS_CODE = "test-to-ods-code";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";
    private static final String EXPECTED_NO_PATIENT_EXCEPTION_MESSAGE = "Missing patient resource in Fhir Bundle.";
    public static final Bundle EMPTY_BUNDLE = new Bundle();
    public static final Bundle BUNDLE_WITHOUT_PATIENT = new Bundle().setEntry(new ArrayList<>());

    private static GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;
    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;
    @Mock
    private EncounterComponentsMapper encounterComponentsMapper;

    @BeforeEach
    public void setUp() throws IOException {
        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(TEST_NHS_NUMBER)
            .conversationId(TEST_CONVERSATION_ID)
            .requestId(TEST_REQUEST_ID)
            .fromOdsCode(TEST_FROM_ODS_CODE)
            .toOdsCode(TEST_TO_ODS_CODE)
            .build();

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID_1, TEST_ID_2, TEST_ID_3);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        messageContext = new MessageContext(randomIdGeneratorService);
        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService,
            timestampService,
            new EncounterMapper(messageContext, encounterComponentsMapper));
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput() throws IOException {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_XML_TO_JSON_FILE);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + JSON_INPUT_FILE);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        assertThat(output).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }

    @ParameterizedTest
    @MethodSource("exceptionParams")
    public void When_MappingInvalidBundleBody_Expect_FhirValidationExceptionThrown(Bundle bundle, String expectedMessage) {
        Exception exception = assertThrows(FhirValidationException.class,
            () -> ehrExtractMapper.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition,
                bundle));
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    private static Stream<Arguments> exceptionParams() {
        return Stream.of(
            Arguments.of(null, EXPECTED_NO_PATIENT_EXCEPTION_MESSAGE),
            Arguments.of(EMPTY_BUNDLE, EXPECTED_NO_PATIENT_EXCEPTION_MESSAGE),
            Arguments.of(BUNDLE_WITHOUT_PATIENT, EXPECTED_NO_PATIENT_EXCEPTION_MESSAGE)
        );
    }
}
