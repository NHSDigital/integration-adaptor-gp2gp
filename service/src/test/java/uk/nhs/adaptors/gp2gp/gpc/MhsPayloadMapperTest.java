package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;

import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.mhs.model.MhsPayloadTemplateParameters;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MhsPayloadMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/gpc/output/";
    private static final String EXPECTED_XML_TO_JSON_FILE = "ExpectedMhsPayload.xml";
    private static final String TEST_MESSAGE_ID = "test-message-id";
    private static final String TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String TEST_REQUEST_ID = "test-request-id";
    private static final String TEST_FROM_ODS_CODE = "test-from-ods-code";
    private static final String TEST_TO_ODS_CODE = "test-to-ods-code";
    private static final String TEST_DOCUMENT_ID = "test-document-id";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";

    private static String expectedJsonToXmlContent;
    private static GetGpcDocumentTaskDefinition getGpcDocumentTaskDefinition;

    private MhsPayloadMapper mhsPayloadMapper;

    @Mock
    private TimestampService timestampService;

    @BeforeAll
    public static void initialize() throws IOException {
        expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + EXPECTED_XML_TO_JSON_FILE);
        getGpcDocumentTaskDefinition = GetGpcDocumentTaskDefinition.builder()
            .conversationId(TEST_CONVERSATION_ID)
            .requestId(TEST_REQUEST_ID)
            .fromOdsCode(TEST_FROM_ODS_CODE)
            .toOdsCode(TEST_TO_ODS_CODE)
            .documentId(TEST_DOCUMENT_ID)
            .build();
    }

    @BeforeEach
    public void setUp() {
        mhsPayloadMapper = new MhsPayloadMapper(timestampService);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
    }

    @Test
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput() {
        MhsPayloadTemplateParameters mhsPayloadTemplateParameters =
            mhsPayloadMapper.mapToMhsPayloadTemplateParameters(getGpcDocumentTaskDefinition, TEST_MESSAGE_ID);
        String output = mhsPayloadMapper.mapMhsPayloadTemplateToXml(mhsPayloadTemplateParameters);

        assertThat(output).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }
}
