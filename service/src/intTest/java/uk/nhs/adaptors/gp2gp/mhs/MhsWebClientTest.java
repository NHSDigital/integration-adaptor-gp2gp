package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    private static MockWebServer mockWebServer;

    @Autowired
    private MhsRequestBuilder mhsRequestBuilder;
    @Autowired
    private MhsClient mhsClient;

    @SpyBean
    private MhsConfiguration mhsConfiguration;

    private static Stream<Arguments> maxAttachmentsValidationErrors() {
        return Stream.of(
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['Longer than maximum length 99.']}"),
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['unknown issue','Longer than "
                + "maximum length 99.']}"),
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': "
                + "['unknown issue','Longer than maximum length 99.'], 'internal_attachments': ['unknown issue']}}")
        );
    }

    private static Stream<Arguments> otherValidationErrors() {
        return Stream.of(
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['unknown issue']}"),
            Arguments.of("400: Invalid request. Validation errors: {'internal_attachments': ['Longer than maximum length 99.']}"),
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['unknown issue'], "
                + "'internal_attachments': ['Longer than maximum length 99.']}")
        );
    }

    @BeforeAll
    public static void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    public void initialise() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        when(mhsConfiguration.getUrl()).thenReturn(baseUrl);
    }


    @ParameterizedTest
    @MethodSource("maxAttachmentsValidationErrors")
    public void When_SendMessageToMHS_With_HttpStatus400AndMaxExternalAttachments_Expect_CorrectException(String body) {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "text/plain")
            .setResponseCode(BAD_REQUEST.value())
            .setBody(body);

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(MaximumExternalAttachmentsException.class);
    }

    @ParameterizedTest
    @MethodSource("otherValidationErrors")
    public void When_SendMessageToMHS_With_HttpStatus400AndOtherValidationErrors_Expect_CorrectException(String body) {
        MockResponse response = new MockResponse();
        response
            .addHeader("Content-Type", "text/plain")
            .setResponseCode(BAD_REQUEST.value())
            .setBody(body);

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(InvalidOutboundMessageException.class);
    }


    @Test
    public void When_SendMessageToMHS_With_HttpStatus404_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .setResponseCode(NOT_FOUND.value());

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void When_SendMessageToMHS_With_HttpStatus5xx_Expect_CorrectException() {
        MockResponse response = new MockResponse();
        response
            .setResponseCode(INTERNAL_SERVER_ERROR.value());

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(MhsServerErrorException.class);
    }
}
