package uk.nhs.adaptors.gp2gp.gpc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GpcDocumentTranslatorTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;
    private static final String BINARY_INPUT_FILE = "test-binary.json";
    private static final String EXPECTED_MHS_OUTBOUND_REQUEST_FILE = "expected-mhs-outbound-request-payload.json";
    private static final String TEST_DOCUMENT_ID = "test-document-id";
    private static final String MESSAGE_ID = "message-id";
    private static final String TEST_ID = "test-id";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";

    private static String jsonBinaryContent;
    private static String expectedMhsOutboundRequest;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;

    private GpcDocumentTranslator gpcDocumentTranslator;

    @BeforeAll
    public static void initialize() throws IOException {
        jsonBinaryContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + BINARY_INPUT_FILE);
        expectedMhsOutboundRequest = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_MHS_OUTBOUND_REQUEST_FILE);
    }

    @BeforeEach
    public void setUp() {
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        gpcDocumentTranslator = new GpcDocumentTranslator(new FhirParseService(),
            new EhrDocumentMapper(timestampService, randomIdGeneratorService));
    }

    @Test
    public void When_TranslatingDocumentData_Expect_ProperMhsOutboundRequestPayload() {
        GetGpcDocumentTaskDefinition taskDefinition = GetGpcDocumentTaskDefinition.builder()
            .documentId(TEST_DOCUMENT_ID)
            .build();
        String payload = gpcDocumentTranslator.translateToMhsOutboundRequestData(taskDefinition, jsonBinaryContent, MESSAGE_ID);

        assertThat(payload).isEqualToIgnoringWhitespace(expectedMhsOutboundRequest);
    }
}
