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

    private DateTime fromDateTime;
    private DateTime toDateTime;
    private String toAsid;
    private String getFromAsid;
    private String toOds;
    private String fromOds;


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
    public void When_EhrStatusEndpointHasContent_Expect_StatusEndpointReturnsAsidCodes() {

        EhrStatusRequest statusRequests = restTemplate.getForObject(ehrStatusRequestsEndpoint, EhrStatusRequest.class);

//        assertThat(status.getFromAsid()).isEqualTo(FROM_ASID);
//        assertThat(status.getToAsid()).isEqualTo(TO_ASID);
    }

    // Test case 1: From Date works as expected
    // Test case 2: To Date Works
    // Test case 3: a combination of from date and to date work as expected
    // Test case 4: From asid works as expected
    // Test case 5: To Asid works as expected
    // Test case 6: From ods works as expected
    // Test case 7: To Ods works as expected


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
