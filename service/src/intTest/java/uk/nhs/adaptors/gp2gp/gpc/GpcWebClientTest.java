package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import uk.nhs.adaptors.gp2gp.common.exception.RetryLimitReachedException;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcTokenBuilder;
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

    private static final MockResponse STUB_OK = initialiseOKResponse();
    private static final MockResponse STUB_INTERNAL_SERVER_ERROR = initialise500Response();
    private static final MockResponse STUB_INTERNAL_SERVER_ERROR_NO_BODY = initialise500ResponseNoBody();
    private static final MockResponse STUB_NO_RESPONSE = initialiseNoResponse();

    private String baseUrl;
    @SpyBean
    private GpcConfiguration gpcConfiguration;
    @SpyBean
    private GpcTokenBuilder gpcTokenBuilder;
    @Autowired
    private GpcClient gpcWebClient;

    private static MockResponse initialiseOKResponse() {
        MockResponse response = new MockResponse();
        response.setResponseCode(OK.value())
            .setBody(TEST_BODY);

        return response;
    }

    private static MockResponse initialise500Response() {
        MockResponse response = new MockResponse();
        response.setResponseCode(INTERNAL_SERVER_ERROR.value());
        response.setBody(TEST_BODY);

        return response;
    }

    private static MockResponse initialise500ResponseNoBody() {
        MockResponse response = new MockResponse();
        response.setResponseCode(INTERNAL_SERVER_ERROR.value());

        return response;
    }

    private static MockResponse initialiseNoResponse() {
        MockResponse response = new MockResponse();
        response.setSocketPolicy(SocketPolicy.NO_RESPONSE);

        return response;
    }

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
        mockWebServer.enqueue(STUB_OK);

        var taskDefinition = buildDocumentTaskDefinition();
        var result = gpcWebClient.getDocumentRecord(taskDefinition);

        assertThat(result).isEqualTo(TEST_BODY);
        verify(gpcTokenBuilder).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetDocumentRecord_With_HttpStatus5xx_Expect_RetryExceptionWithGpcServerErrorExceptionAsRootCause() {
        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(STUB_INTERNAL_SERVER_ERROR);
        }

        var taskDefinition = buildDocumentTaskDefinition();

        assertThatThrownBy(() -> gpcWebClient.getDocumentRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(GpcServerErrorException.class)
            .hasRootCauseMessage("The following error occurred during GPC request: " + TEST_BODY);


        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
        verify(gpcTokenBuilder, times(FOUR)).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetDocumentRecord_With_HttpStatus5xxAndNoBody_Expect_AlternativeExceptionMessage() {
        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(STUB_INTERNAL_SERVER_ERROR_NO_BODY);
        }

        var taskDefinition = buildDocumentTaskDefinition();

        assertThatThrownBy(() -> gpcWebClient.getDocumentRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(GpcServerErrorException.class)
            .hasRootCauseMessage("The following error occurred during GPC request: 500 INTERNAL_SERVER_ERROR");


        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
        verify(gpcTokenBuilder, times(FOUR)).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetDocumentRecord_With_NoResponse_Expect_RetryExceptionWithTimeoutAsRootCause() {
        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(STUB_NO_RESPONSE);
        }

        var taskDefinition = buildDocumentTaskDefinition();

        assertThatThrownBy(() -> gpcWebClient.getDocumentRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasRootCauseInstanceOf(TimeoutException.class)
            .hasMessage("Retries exhausted: 3/3");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
        verify(gpcTokenBuilder, times(FOUR)).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetDocumentRecord_With_NoResponseBeforeHttpStatus200_Expect_RetryBeforeSuccess() {
        mockWebServer.enqueue(STUB_NO_RESPONSE);
        mockWebServer.enqueue(STUB_OK);

        var taskDefinition = buildDocumentTaskDefinition();
        var result = gpcWebClient.getDocumentRecord(taskDefinition);

        assertThat(result).isEqualTo(TEST_BODY);
        verify(gpcTokenBuilder, times(2)).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetStructuredRecord_With_HttpStatus200_Expect_NoException() {

        mockWebServer.enqueue(STUB_OK);

        var taskDefinition = getStructuredDefinition();
        var result = gpcWebClient.getStructuredRecord(taskDefinition);

        assertThat(result).isEqualTo(TEST_BODY);
        verify(gpcTokenBuilder).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetStructuredRecord_With_NoResponse_Expect_RetryExceptionWithTimeoutAsRootCause() {
        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(STUB_NO_RESPONSE);
        }

        var taskDefinition = getStructuredDefinition();

        assertThatThrownBy(() -> gpcWebClient.getStructuredRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasRootCauseInstanceOf(TimeoutException.class)
            .hasMessage("Retries exhausted: 3/3");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
        verify(gpcTokenBuilder, times(FOUR)).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    public void When_GetStructuredRecord_With_HttpStatus5xx_Expect_RetryWithGpcServerErrorExceptionAsRootCause() {
        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(STUB_INTERNAL_SERVER_ERROR);
        }

        var taskDefinition = getStructuredDefinition();

        assertThatThrownBy(() -> gpcWebClient.getStructuredRecord(taskDefinition))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(GpcServerErrorException.class)
            .hasRootCauseMessage("The following error occurred during GPC request: " + TEST_BODY);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
        verify(gpcTokenBuilder, times(FOUR)).buildToken(taskDefinition.getFromOdsCode());
    }

    @Test
    void When_GetStructuredRecord_With_HttpStatus200_Expect_AuthorizationHeaderToBePresent() throws InterruptedException {
        // given
        final GetGpcStructuredTaskDefinition taskDefinition = getStructuredDefinition();
        final String fromOdsCode = taskDefinition.getFromOdsCode();
        final Collection<String> tokens = new ArrayList<>();

        // when
        doAnswer(invocation -> {
            final String tokenFromSpy = (String) invocation.callRealMethod();
            tokens.add("Bearer %s".formatted(tokenFromSpy)); // Add initial token generated by gpcTokenBuilder.buildToken();
            return tokenFromSpy;
        }).when(gpcTokenBuilder).buildToken(fromOdsCode);

        mockWebServer.enqueue(STUB_OK);
        gpcWebClient.getStructuredRecord(taskDefinition);
        tokens.add(Objects.requireNonNull(mockWebServer
                        .takeRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION))); // Add token from Authorisation header (WebClient call).

        // then
        final long distinctTokens = tokens.stream().distinct().count();
        verify(gpcTokenBuilder).buildToken(fromOdsCode);
        assertThat(tokens).hasSize(2).doesNotContainNull();
        assertThat(distinctTokens).isEqualTo(1);
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
