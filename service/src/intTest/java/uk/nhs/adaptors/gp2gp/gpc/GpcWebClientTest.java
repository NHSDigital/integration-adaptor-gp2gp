package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
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
import okhttp3.mockwebserver.SocketPolicy;
import uk.nhs.adaptors.gp2gp.common.exception.RetryLimitReachedException;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpcServerErrorException;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class, MongoDBExtension.class})
@DirtiesContext
public class GpcWebClientTest {
    private static final String DOCUMENT_ID = "testDocumentId";
    private static final String MESSAGE_ID = "testMessageId";
    private static final String FROM_ASID = "testFromAsid";
    private static final String TO_ASID = "testToAsid";
    private static final String FROM_ODS = "testFromOdsCode";
    private static final String TO_ODS = "testToOdsCode";
    private static final String CONVERSATION_ID = "testConversationId";
    private static final String REQUEST_ID = "testRequestId";
    private static final String TASK_ID = "testTaskId";
    private static final String TEST_BODY = "Test Body";
    private static final int FOUR = 4;

    private static MockWebServer mockWebServer;

    private String baseUrl;
    @SpyBean
    private GpcConfiguration gpcConfiguration;
    @Autowired
    private GpcClient gpcWebClient;

    @BeforeEach
    public void initialise() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        when(gpcConfiguration.getUrl()).thenReturn(baseUrl);
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void When_GetDocumentRecord_With_HttpStatus200_Expect_NoException() {
        MockResponse response = new MockResponse();
        response.setResponseCode(OK.value())
            .setBody(TEST_BODY);

        mockWebServer.enqueue(response);

        var taskDefinition = buildDocumentTaskDefinition();
        var result = gpcWebClient.getDocumentRecord(taskDefinition);

        assertThat(result).isEqualTo(TEST_BODY);
    }

    @Test
    public void When_GetDocumentRecord_With_HttpStatus5xx_Expect_RetryExceptionWithGpcServerErrorExceptionAsRootCause() {
        MockResponse response = new MockResponse();
        response.setResponseCode(INTERNAL_SERVER_ERROR.value());
        response.setBody(TEST_BODY);

        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);

        var taskDefinition = buildDocumentTaskDefinition();

        assertThatThrownBy(() -> gpcWebClient.getDocumentRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(GpcServerErrorException.class)
            .hasRootCauseMessage("The following error occurred during GPC request: " + TEST_BODY);


        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
    }

    @Test
    public void When_GetDocumentRecord_With_NoResponse_Expect_RetryExceptionWithTimeoutAsRootCause() {
        MockResponse response = new MockResponse();
        response.setSocketPolicy(SocketPolicy.NO_RESPONSE);

        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);

        var taskDefinition = buildDocumentTaskDefinition();

        assertThatThrownBy(() -> gpcWebClient.getDocumentRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasRootCauseInstanceOf(TimeoutException.class)
            .hasMessage("Retries exhausted: 3/3");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
    }

    @Test
    public void When_GetDocumentRecord_With_NoResponseBeforeHttpStatus200_Expect_RetryBeforeSuccess() {
        MockResponse response1 = new MockResponse();
        response1.setSocketPolicy(SocketPolicy.NO_RESPONSE);
        MockResponse response2 = new MockResponse();
        response2.setResponseCode(OK.value());
        response2.setBody(TEST_BODY);

        mockWebServer.enqueue(response1);
        mockWebServer.enqueue(response2);

        var taskDefinition = buildDocumentTaskDefinition();
        var result = gpcWebClient.getDocumentRecord(taskDefinition);

        assertThat(result).isEqualTo(TEST_BODY);
    }

    @Test
    public void When_GetStructuredRecord_With_HttpStatus200_Expect_NoException() {
        MockResponse response = new MockResponse();
        response.setResponseCode(OK.value())
            .setBody(TEST_BODY);

        mockWebServer.enqueue(response);

        var taskDefinition = getStructuredDefinition();
        var result = gpcWebClient.getStructuredRecord(taskDefinition);

        assertThat(result).isEqualTo(TEST_BODY);
    }

    @Test
    public void When_GetStructuredRecord_With_NoResponse_Expect_RetryExceptionWithTimeoutAsRootCause() {
        MockResponse response = new MockResponse();
        response.setSocketPolicy(SocketPolicy.NO_RESPONSE);

        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);

        var taskDefinition = getStructuredDefinition();

        assertThatThrownBy(() -> gpcWebClient.getStructuredRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasRootCauseInstanceOf(TimeoutException.class)
            .hasMessage("Retries exhausted: 3/3");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
    }

    @Test
    public void When_GetStructuredRecord_With_HttpStatus5xx_Expect_RetryWithGpcServerErrorExceptionAsRootCause() {
        MockResponse response = new MockResponse();
        response.setResponseCode(INTERNAL_SERVER_ERROR.value());
        response.setBody(TEST_BODY);

        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);

        var taskDefinition = getStructuredDefinition();

        assertThatThrownBy(() -> gpcWebClient.getStructuredRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(GpcServerErrorException.class)
            .hasRootCauseMessage("The following error occurred during GPC request: " + TEST_BODY);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
    }

    private GetGpcDocumentTaskDefinition buildDocumentTaskDefinition() {
        return GetGpcDocumentTaskDefinition.builder()
            .messageId(MESSAGE_ID)
            .fromAsid(FROM_ASID)
            .toAsid(TO_ASID)
            .fromOdsCode(FROM_ODS)
            .toOdsCode(TO_ODS)
            .conversationId(CONVERSATION_ID)
            .requestId(REQUEST_ID)
            .taskId(TASK_ID)
            .documentId(DOCUMENT_ID)
            .accessDocumentUrl(String.format("%s/%s/STU3/1/gpconnect/documents/fhir/Binary/%s",
                baseUrl, TO_ODS, DOCUMENT_ID))
            .build();
    }

    private GetGpcStructuredTaskDefinition getStructuredDefinition() {
        return GetGpcStructuredTaskDefinition.builder()
            .nhsNumber("1234")
            .taskId(TASK_ID)
            .conversationId(CONVERSATION_ID)
            .requestId(REQUEST_ID)
            .fromAsid(FROM_ASID)
            .toAsid(TO_ASID)
            .toOdsCode(TO_ODS)
            .fromOdsCode(FROM_ODS)
            .build();
    }
}
