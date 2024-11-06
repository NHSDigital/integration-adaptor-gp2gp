package uk.nhs.adaptors.gp2gp.gpc;

import org.hl7.fhir.dstu3.model.Binary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.GetAbsentAttachmentTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentToMHSTranslatorTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;

    private static final String TEST_DOCUMENT_ID = "test-document-id";
    private static final String MESSAGE_ID = "message-id";
    private static final String TEST_ID = "test-id";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";
    private static final String TEST_TASK_ID = "test-task-id";
    private static final String TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String TEST_TO_ODS = "test-to-ods-code";
    private static final String TEST_TO_ASID = "test-to-asid";
    private static final String TEST_TITLE = "Test DocumentReference.content.attachment.title";

    private static final String BINARY_INPUT_FILE = "test-binary.json";
    private static final String ABSENT_ATTACHMENT_INPUT_FILE = "absent-attachment-test.txt";

    private static final String EXPECTED_MHS_OUTBOUND_REQUEST_FILE = "expected-mhs-outbound-request-payload.json";
    private static final String EXPECTED_MHS_OUTBOUND_ABSENT_ATTACHMENT_FILE = "expected-mhs-outbound-absentattachment-payload.json";

    private static String jsonBinaryContent;
    private static String absentAttachmentTxtContent;
    private static String expectedMhsOutboundRequest;
    private static String expectedAbsentAttachmentPayload;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;

    private DocumentToMHSTranslator documentToMHSTranslator;

    @BeforeAll
    public static void initialize() throws IOException {
        jsonBinaryContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + BINARY_INPUT_FILE);
        absentAttachmentTxtContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + ABSENT_ATTACHMENT_INPUT_FILE);
        expectedMhsOutboundRequest = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_MHS_OUTBOUND_REQUEST_FILE);
        expectedAbsentAttachmentPayload = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_MHS_OUTBOUND_ABSENT_ATTACHMENT_FILE);
    }

    @BeforeEach
    public void setUp() {
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        documentToMHSTranslator = new DocumentToMHSTranslator(
            new ObjectMapper(),
            new EhrDocumentMapper(timestampService, randomIdGeneratorService));
    }

    @Test
    public void When_TranslatingDocumentData_Expect_ProperMhsOutboundRequestPayload() {
        GetGpcDocumentTaskDefinition taskDefinition = GetGpcDocumentTaskDefinition.builder()
            .messageId(MESSAGE_ID)
            .documentId(TEST_DOCUMENT_ID)
            .build();
        Binary binary = new FhirParseService().parseResource(jsonBinaryContent, Binary.class);
        String payload = documentToMHSTranslator.translateGpcResponseToMhsOutboundRequestData(
            taskDefinition, binary.getContentAsBase64(), binary.getContentType());

        assertThat(payload).isEqualToIgnoringWhitespace(expectedMhsOutboundRequest);
    }

    @Test
    public void When_TranslatingFileContentData_Expect_ProperMhsOutboundRequestPayload() {
        final GetAbsentAttachmentTaskDefinition taskDefinition = GetAbsentAttachmentTaskDefinition.builder()
            .title(TEST_TITLE)
            .messageId(MESSAGE_ID)
            .documentId(TEST_DOCUMENT_ID)
            .taskId(TEST_TASK_ID)
            .conversationId(TEST_CONVERSATION_ID)
            .toOdsCode(TEST_TO_ODS)
            .toAsid(TEST_TO_ASID)
            .build();

        String mhsOutboundRequestData = documentToMHSTranslator.translateFileContentToMhsOutboundRequestData(
            taskDefinition, Base64Utils.toBase64String(absentAttachmentTxtContent)
        );
        assertThat(mhsOutboundRequestData).isEqualToIgnoringWhitespace(expectedAbsentAttachmentPayload);
    }
}
