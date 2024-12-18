package uk.nhs.adaptors.gp2gp.ehr.status;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.ehr.InboundMessageHandlingTest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.util.ProcessDetectionService;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EhrStatusRequestsEndpointTests {

    private static final String INBOUND_QUEUE_NAME = "gp2gpInboundQueue";
    private static final String EBXML_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_ebxml.txt";
    private static final String PAYLOAD_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_payload.txt";

    private static final int JMS_RECEIVE_TIMEOUT = 60000;
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private static final int NUMBER_OF_REQUESTS = 5;

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessFailureHandlingService processFailureHandlingService;

    private String ehrStatusRequestsEndpoint;

    private static final Instant[] FROM_DATE_TIMES = new Instant[NUMBER_OF_REQUESTS];
    private static final Instant[] TO_DATE_TIMES = new Instant[NUMBER_OF_REQUESTS];
    private static final String[] TO_ASIDS = new String[NUMBER_OF_REQUESTS];
    private static final String[] FROM_ASIDS = new String[NUMBER_OF_REQUESTS];
    private static final String[] TO_ODSS = new String[NUMBER_OF_REQUESTS];
    private static final String[] FROM_ODSS = new String[NUMBER_OF_REQUESTS];
    private static final String[] CONVERSATION_IDS = new String[NUMBER_OF_REQUESTS];

    private static final String CONVERSATION_ID_PLACEHOLDER = "{{conversationId}}";
    private static final String TO_ASID_PLACEHOLDER = "{{toAsid}}";
    private static final String FROM_ASID_PLACEHOLDER = "{{fromAsid}}";
    private static final String FROM_ODS_PLACEHOLDER = "{{fromOds}}";
    private static final String TO_ODS_PLACEHOLDER = "{{toOds}}";

    @BeforeEach
    public void setUp() {

        inboundJmsTemplate.setDefaultDestinationName(INBOUND_QUEUE_NAME);
        ehrStatusRequestsEndpoint = "http://localhost:" + port + "/requests";
        inboundJmsTemplate.setReceiveTimeout(JMS_RECEIVE_TIMEOUT);

        for (var i = 0; i < NUMBER_OF_REQUESTS; i++) {

            FROM_DATE_TIMES[i] = Instant.now();
            var inboundMessage = new InboundMessage();

            var conversationId = UUID.randomUUID().toString();
            var fromAsid = UUID.randomUUID().toString();
            var toAsid = UUID.randomUUID().toString();
            var fromOds = UUID.randomUUID().toString();
            var toOds = UUID.randomUUID().toString();

            var payload = readResourceAsString(PAYLOAD_PATH_REQUEST_MESSAGE);
            payload = payload
                .replace(FROM_ASID_PLACEHOLDER, fromAsid)
                .replace(TO_ASID_PLACEHOLDER, toAsid)
                .replace(FROM_ODS_PLACEHOLDER, fromOds)
                .replace(TO_ODS_PLACEHOLDER, toOds);

            var ebxml = readResourceAsString(EBXML_PATH_REQUEST_MESSAGE).replace(CONVERSATION_ID_PLACEHOLDER, conversationId);

            inboundMessage.setEbXML(ebxml);
            inboundMessage.setPayload(payload);

            inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));

            await()
                .atMost(ONE_MINUTE)
                .until(() -> processDetectionService.awaitingContinue(conversationId));

            processFailureHandlingService.failProcess(conversationId, "test error code", "test error message", "test task type");

            await()
                .until(() -> processDetectionService.processFailed(conversationId));

            TO_DATE_TIMES[i] = Instant.now();
            FROM_ASIDS[i] = fromAsid;
            TO_ASIDS[i] = toAsid;
            FROM_ODSS[i] = fromOds;
            TO_ODSS[i] = toOds;
            CONVERSATION_IDS[i] = conversationId;
        }
    }

    @Autowired
    private JmsTemplate inboundJmsTemplate;

    @Autowired
    private ProcessDetectionService processDetectionService;

    @Test
    public void When_EhrStatusEndpointHasContentAndNoFiltersAreApplied_Expect_StatusEndpointReturnsAllExpectedResponses() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(NUMBER_OF_REQUESTS);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoToDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // If we take the from timestamp of the second created item we should get n - 1 responses where n is the number of data items
        // created for these tests;
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[1])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(NUMBER_OF_REQUESTS - 1);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoFromDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // If we take the to timestamp of the first created item we should only get the first data item from our test data
        var queryRequest = EhrStatusRequestQuery.builder()
            .toDateTime(TO_DATE_TIMES[0])
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[0], responseEntity);
    }

    @Disabled
    @Test
    public void When_EhrStatusEndpointHasContentAndBothToAndFromDateFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        // We are excluding the first and last data item in our test data meaning we should get n-2 results where n is the number of test
        // data items
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[1])
            .toDateTime(TO_DATE_TIMES[TO_DATE_TIMES.length - 2])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(NUMBER_OF_REQUESTS - 2);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndFromASIDFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .fromAsid(FROM_ASIDS[1])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[1], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndToASIDFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toAsid(TO_ASIDS[3])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[3], responseEntity);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndFromODSFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .fromOdsCode(FROM_ODSS[1])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[1], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndToODSFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toOdsCode(TO_ODSS[4])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[4], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndMultipleFiltersApplied_Expect_ExpectedRangeOfResponsesReturned() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[0])
            .toDateTime(TO_DATE_TIMES[NUMBER_OF_REQUESTS - 1])
            .toAsid(TO_ASIDS[3])
            .fromOdsCode(FROM_ODSS[3])
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[3], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndFiltersHaveNoResult_Expect_StatusEndpointReturnsA204Response() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[0])
            .toDateTime(TO_DATE_TIMES[NUMBER_OF_REQUESTS - 1])
            .toAsid(TO_ASIDS[3])
            .fromOdsCode(FROM_ODSS[2])
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void When_EhrStatusEndpointResponseHasNoContent_Expect_StatusEndpointReturnsA204Response() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toDateTime(FROM_DATE_TIMES[0].minus(Duration.ofDays(1)))
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @SneakyThrows
    private String parseMessageToString(InboundMessage inboundMessage) {
        return objectMapper.writeValueAsString(inboundMessage);
    }

    @SneakyThrows
    private static String readResourceAsString(String path) {
        try (InputStream is = InboundMessageHandlingTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new FileNotFoundException(path);
            }
            return IOUtils.toString(is, UTF_8);
        }
    }

    private void assertOneResponseWithConversationId(String conversationId, ResponseEntity<EhrStatusRequest[]> responseEntity) {
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(1);
        assertThat(statusRequests[0].getConversationId()).isEqualTo(conversationId);
    }
}
