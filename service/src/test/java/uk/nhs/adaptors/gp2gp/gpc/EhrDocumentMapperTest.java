package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class EhrDocumentMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/gpc/output/";
    private static final String EXPECTED_XML_TO_JSON_FILE = "ExpectedEhrDocument.xml";
    private static final String TEST_MESSAGE_ID = "test-message-id";
    private static final String TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String TEST_REQUEST_ID = "test-request-id";
    private static final String TEST_FROM_ASID_CODE = "test-from-asid-code";
    private static final String TEST_TO_ASID_CODE = "test-to-asid-code";
    private static final String TEST_FROM_ODS_CODE = "test-from-ods-code";
    private static final String TEST_TO_ODS_CODE = "test-to-ods-code";
    private static final String TEST_DOCUMENT_ID = "test-document-id";
    private static final String TEST_ID = "test-id";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEST_FILENAME = "test-document-id.txt";

    private final String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + EXPECTED_XML_TO_JSON_FILE);
    private final GetGpcDocumentTaskDefinition getGpcDocumentTaskDefinition = GetGpcDocumentTaskDefinition.builder()
            .conversationId(TEST_CONVERSATION_ID)
            .messageId(TEST_MESSAGE_ID)
            .requestId(TEST_REQUEST_ID)
            .fromAsid(TEST_FROM_ASID_CODE)
            .toAsid(TEST_TO_ASID_CODE)
            .fromOdsCode(TEST_FROM_ODS_CODE)
            .toOdsCode(TEST_TO_ODS_CODE)
            .documentId(TEST_DOCUMENT_ID)
            .build();

    private EhrDocumentMapper ehrDocumentMapper;

    @Mock
    private TimestampService timestampService;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @BeforeEach
    public void setUp() {
        ehrDocumentMapper = new EhrDocumentMapper(timestampService, randomIdGeneratorService);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
    }

    @Test
    public void When_MappingJsonRequestBody_WithDocumentId_Expect_XmlOutput() {
        assertThat(ehrDocumentMapper.generateMhsPayload(
            getGpcDocumentTaskDefinition,
            TEST_MESSAGE_ID,
            TEST_DOCUMENT_ID,
            TEXT_PLAIN
        )).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }

    @Test
    public void When_MappingJsonRequestBody_WithFilename_Expect_XmlOutput() {
        assertThat(ehrDocumentMapper.generateMhsPayload(
                getGpcDocumentTaskDefinition,
                TEST_MESSAGE_ID,
                TEST_FILENAME
        )).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }
}
