package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import uk.nhs.adaptors.gp2gp.common.exception.MaximumExternalAttachmentsException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class, MongoDBExtension.class})
@DirtiesContext
public class MhsWebClientTest {

    private static final String TEST_BODY = "test body";
    private static final String TEST_CONVERSATION_ID = "test conversation id";
    private static final String TEST_FROM_ODS_CODE = "test from ods code";
    private static final String TEST_MESSAGE_ID = "test message id";

    public static MockWebServer mockWebServer;

    @Autowired
    private MhsRequestBuilder mhsRequestBuilder;
    @Autowired
    private MhsClient mhsClient;

    @SpyBean
    private MhsConfiguration mhsConfiguration;

    @BeforeAll
    static void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialise() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        when(mhsConfiguration.getUrl()).thenReturn(baseUrl);
    }

    @Test
    public void When_SendMessageToMHS_With_400AndMaxExternalAttachments_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "application/json")
            .setResponseCode(400)
            .setBody("{\"external_attachments\": [\"Longer than maximum length 99.\"]}");

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(MaximumExternalAttachmentsException.class);
    }

    @Test
    public void When_SendMessageToMHS_With_400AndOtherValidationErrors_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "application/json")
            .setResponseCode(400)
            .setBody("{\"unknown_verification_error\": [\"test problem.\"]}");

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(InvalidOutboundMessageException.class);
    }

    @Test
    public void When_SendMessageToMHS_With_400AndInvalidJson_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "application/json")
            .setResponseCode(400)
            .setBody("Invalid JSON body");

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(InvalidOutboundMessageException.class);
    }

    @Test
    public void When_SendMessageToMHS_With_404_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "application/json")
            .setResponseCode(404)
            .setBody("Not found");

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(InvalidOutboundMessageException.class);
    }

    @Test
    public void When_SendMessageToMHS_With_5xx_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "application/json")
            .setResponseCode(500)
            .setBody("Server Error");

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(MhsServerErrorException.class);
    }
}
