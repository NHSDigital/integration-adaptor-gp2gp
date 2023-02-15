package uk.nhs.adaptors.gp2gp.ehr.status;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.ehr.InboundMessageHandlingTest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.util.ProcessDetectionService;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class EhrStatusRequestsEndpointTests {

    private static final String INBOUND_QUEUE_NAME = "inbound";
    private static final String EBXML_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_ebxml.txt";
    private static final String PAYLOAD_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_payload.txt";
    private static final String TO_ASID = "715373337545";
    private static final String FROM_ASID = "276827251543";
    private static final int JMS_RECEIVE_TIMEOUT = 60000;
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration THREE_SECONDS = Duration.ofSeconds(3);

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestRestTemplate restTemplate;
    private String ehrStatusRequestsEndpoint;
    private Boolean setupComplete = false;

    private DateTime[] fromDateTimes;
    private DateTime[] toDateTimes;
    private String[] toAsids;
    private String[] fromAsids;
    private String[] toOdss;
    private String[] fromOdss;


    private static final String CONVERSATION_ID_PLACEHOLDER = "{{conversationId}}";
    private static final String TO_ASID_PLACEHOLDER = "{{toAsid}}";
    private static final String FROM_ASID_PLACEHOLDER = "{{fromAsid}}";

    @Before
    public void setUp(){
        if (setupComplete == false) {
            inboundJmsTemplate.setDefaultDestinationName(INBOUND_QUEUE_NAME);
            ehrStatusRequestsEndpoint = "http://localhost:" + port + "/requests";
            inboundJmsTemplate.setReceiveTimeout(JMS_RECEIVE_TIMEOUT);

            for(var i =0; i < 5; i++) {

                var inboundMessage = new InboundMessage();

                var conversationId = UUID.randomUUID().toString();
                var fromAsid = UUID.randomUUID().toString();
                var toAsid = UUID.randomUUID().toString();

                var payload = readResourceAsString(PAYLOAD_PATH_REQUEST_MESSAGE);
                payload = payload.replace(FROM_ASID_PLACEHOLDER, fromAsid);
                payload = payload.replace(TO_ASID_PLACEHOLDER, toAsid);

                var ebxml = readResourceAsString(EBXML_PATH_REQUEST_MESSAGE).replace(CONVERSATION_ID_PLACEHOLDER, conversationId);

                inboundMessage.setEbXML(ebxml);
                inboundMessage.setPayload(payload);

                inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));

                await()
                    .atMost(ONE_MINUTE)
                    .pollInterval(THREE_SECONDS)
                    .until(() -> processDetectionService.awaitingContinue(conversationId));
            }
        }
        setupComplete = true;
    }

    @Autowired
    private JmsTemplate inboundJmsTemplate;

    @Autowired
    private ProcessDetectionService processDetectionService;

    @Test
    public void When_EhrStatusEndpointHasContentAndNoFilltersAreApplied_Expect_StatusEndpointReturnsAllExpectedResponses() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoFromDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // If we take the from timestamp of the second created item we should get n - 1 responses where n is the number of data items created for these tests;
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(fromDateTimes[1])
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoToDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // If we take the to timestamp of the first created item we should only get the first data item from our test data
        var queryRequest = EhrStatusRequestQuery.builder()
            .toDateTime(toDateTimes[0])
            .build();

        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointHasContentAndBothToAndFromDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // We are excluding the first and last data item in our test data meaning we should get n-2 results where n is the number of test data items
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(fromDateTimes[1])
            .toDateTime(toDateTimes[toDateTimes.length-1])
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointHasContentAndFromASIDFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .fromAsid(fromAsids[1])
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointHasContentAndToASIDFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toAsid(toAsids[3])
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointHasContentAndFromODSFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .fromOdsCode(fromOdss[1])
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointHasContentAndToODSFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toOdsCode(toOdss[4])
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

    }

    @Test
    public void When_EhrStatusEndpointResponseHasNoContent_Expect_StatusEndpointReturnsA204Response() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toDateTime(fromDateTimes[0].minusDays(1))
            .build();
        EhrStatusRequest statusRequests = restTemplate.postForObject(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest.class);

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
}
