package uk.nhs.adaptors.gp2gp.ehr.status;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
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
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.util.ProcessDetectionService;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class EhrStatusEndpointTest {

    private static final String INBOUND_QUEUE_NAME = "gp2gpInboundQueue";
    private static final String EBXML_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_ebxml.txt";
    private static final String PAYLOAD_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_payload.txt";
    private static final String TO_ASID_PLACEHOLDER = "{{toAsid}}";
    private static final String FROM_ASID_PLACEHOLDER = "{{fromAsid}}";
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
    private String conversationId;
    private String ehrStatusEndpoint;

    private static final String CONVERSATION_ID_PLACEHOLDER = "{{conversationId}}";

    @BeforeEach
    public void setUp() {
        inboundJmsTemplate.setDefaultDestinationName(INBOUND_QUEUE_NAME);
        conversationId = UUID.randomUUID().toString();
        ehrStatusEndpoint = "http://localhost:" + port + "/ehr-status/" + conversationId;

        inboundJmsTemplate.setReceiveTimeout(JMS_RECEIVE_TIMEOUT);
    }

    @Autowired
    private JmsTemplate inboundJmsTemplate;

    @Autowired
    private ProcessDetectionService processDetectionService;

    @Test
    public void When_EhrStatusEndpointHasContent_Expect_StatusEndpointReturnsAsidCodes() {

        var inboundMessage = new InboundMessage();
        var payload = readResourceAsString(PAYLOAD_PATH_REQUEST_MESSAGE);
        var ebxml = readResourceAsString(EBXML_PATH_REQUEST_MESSAGE).replace(CONVERSATION_ID_PLACEHOLDER, conversationId);

        payload = payload.replace(FROM_ASID_PLACEHOLDER, FROM_ASID)
            .replace(TO_ASID_PLACEHOLDER, TO_ASID);

        inboundMessage.setEbXML(ebxml);
        inboundMessage.setPayload(payload);

        inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));

        await()
            .atMost(ONE_MINUTE)
            .pollInterval(THREE_SECONDS)
            .until(() -> processDetectionService.awaitingContinue(conversationId));

        EhrStatus status = restTemplate.getForObject(ehrStatusEndpoint, EhrStatus.class);

        assertThat(status.getFromAsid()).isEqualTo(FROM_ASID);
        assertThat(status.getToAsid()).isEqualTo(TO_ASID);
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
