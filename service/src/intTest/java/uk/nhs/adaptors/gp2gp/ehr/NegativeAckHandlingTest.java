package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static uk.nhs.adaptors.gp2gp.common.ResourceReader.asString;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_INCUMBENT;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.IN_PROGRESS;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.util.ProcessDetectionService;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class NegativeAckHandlingTest {

    private static final String MESSAGE_ID_PLACEHOLDER = "{{messageId}}";
    private static final String CONVERSATION_ID_PLACEHOLDER = "{{conversationId}}";
    private static final String TO_ASID_PLACEHOLDER = "{{toAsid}}";
    private static final String FROM_ASID_PLACEHOLDER = "{{fromAsid}}";
    private static final String REASON_CODE_PLACEHOLDER = "{{nackCode}}";
    private static final String REASON_DISPLAY_PLACEHOLDER = "{{nackDisplay}}";
    private static final String MESSAGE_REF_PLACEHOLDER = "{{messageRef}}";
    private static final String SENDER_ASID = "1234";
    private static final String REQUESTOR_ASID = "5678";
    private static final String INBOUND_QUEUE_NAME = "gp2gpInboundQueue";
    private static final int JMS_RECEIVE_TIMEOUT = 60000;

    @Autowired
    private JmsTemplate inboundJmsTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProcessDetectionService processDetectionService;
    @Autowired
    private EhrExtractStatusService extractStatusService;

    @Autowired
    private EhrExtractStatusRepository extractStatusRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("classpath:/requestmessage/RCMR_IN010000UK05_payload.txt")
    private Resource requestPayload;

    @Value("classpath:/requestmessage/RCMR_IN010000UK05_ebxml.txt")
    private Resource requestEbxml;

    @Value("classpath:/inbound/acknowledgement/templates/MCCI_IN010000UK13_ebxml.txt")
    private Resource ackHeader;

    @Value("classpath:/inbound/acknowledgement/templates/MCCI_IN010000UK13_nack_payload.txt")
    private Resource nackPayload;

    @Value("classpath:/continuemessage/COPC_IN000001UK01_ebxml.txt")
    private Resource continueEbxml;

    @Value("classpath:/continuemessage/COPC_IN000001UK01_payload.txt")
    private Resource continuePayload;

    @LocalServerPort
    private int port;
    private String conversationId;

    private String ehrStatusEndpoint;

    @BeforeEach
    public void setup() {
        inboundJmsTemplate.setDefaultDestinationName(INBOUND_QUEUE_NAME);
        conversationId = UUID.randomUUID().toString().toUpperCase();
        ehrStatusEndpoint = "http://localhost:" + port + "/ehr-status/" + conversationId;
        inboundJmsTemplate.setReceiveTimeout(JMS_RECEIVE_TIMEOUT);
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode12BeforeContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyBeforeContinue("12", "Duplicate EHR Extract received");
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode21BeforeContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyBeforeContinue("21", "EHR Extract message not well-formed or not able to be processed");
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode17BeforeContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyBeforeContinue("17",
            "A-B-A EHR Extract Received and rejected due to wrong record or wrong patient");
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode28BeforeContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyBeforeContinue("28",
            "Non A-B-A EHR Extract Received and rejected due to wrong record or wrong patient");
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode31AfterContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyAfterContinue("31",
            "The overall EHR Extract has been rejected because one or more attachments via Large Messages were not received.");
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode11AfterContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyAfterContinue("11",
            "Failed to successfully integrate EHR Extract.");
    }

    @Test
    public void When_FailedByRequestor_With_ReasonCode15AfterContinue_Expect_CorrectMigrationStatus() {
        assertEhrNackIsHandledCorrectlyAfterContinue("15",
            "A-B-A EHR Extract Received and Stored As Suppressed Record");
    }

    @Test
    public void When_FailedByRequestor_With_COPCNackWithReasonCode25_Expect_CorrectMigrationStatus() {
        assertLargeMessageCOPCNackIsHandledCorrectly("25",
            "Large messages rejected due to timeout duration reached of overall transfer");
    }

    @Test
    public void When_FailedByRequestor_With_COPCNackWithReasonCode29_Expect_CorrectMigrationStatus() {
        assertLargeMessageCOPCNackIsHandledCorrectly("29", "Large Message general failure");
    }

    @Test
    public void When_FailedByRequestor_With_COPCNackWithReasonCode30_Expect_CorrectMigrationStatus() {
        assertLargeMessageCOPCNackIsHandledCorrectly("30", "Large Message Re-assembly failure");
    }

    private void assertEhrNackIsHandledCorrectlyAfterContinue(String reasonCode, String reasonDisplay) {
        sendRequestToQueue();
        await().until(() -> processDetectionService.awaitingContinue(conversationId));

        sendContinueToQueue();
        await().until(() -> processDetectionService.awaitingAck(conversationId));

        String ehrMessageId = extractStatusService.fetchEhrExtractMessageId(conversationId).orElseThrow();
        sendNackToQueue(reasonCode, reasonDisplay, ehrMessageId);

        await().until(() -> processDetectionService.processFailed(conversationId));

        EhrStatus ehrStatus = retrieveEhrStatus();
        assertThat(ehrStatus.getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    private void assertEhrNackIsHandledCorrectlyBeforeContinue(String reasonCode, String reasonDisplay) {
        sendRequestToQueue();
        await().until(() -> processDetectionService.awaitingContinue(conversationId));
        String ehrMessageId = extractStatusService.fetchEhrExtractMessageId(conversationId).orElseThrow();

        sendNackToQueue(reasonCode, reasonDisplay, ehrMessageId);
        await().until(() -> processDetectionService.processFailed(conversationId));

        EhrStatus ehrStatus = retrieveEhrStatus();
        assertThat(ehrStatus.getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    private void assertLargeMessageCOPCNackIsHandledCorrectly(String reasonCode, String reasonDisplay) {
        sendRequestToQueue();
        await().until(() -> processDetectionService.awaitingContinue(conversationId));

        sendContinueToQueue();
        await().until(() -> processDetectionService.awaitingAck(conversationId));
        String copcMessageId = fetchFirstCopcMessageId();

        sendNackToQueue(reasonCode, reasonDisplay, copcMessageId);

        await().until(() -> processDetectionService.nackReceived(conversationId));

        EhrStatus ehrStatus = retrieveEhrStatus();
        assertThat(ehrStatus.getMigrationStatus()).isEqualTo(IN_PROGRESS);

        String ehrMessageId = extractStatusService.fetchEhrExtractMessageId(conversationId).orElseThrow();
        sendNackToQueue("31", "The overall EHR Extract has been rejected because one or more attachments via " +
            "Large Messages were not received.", ehrMessageId);

        await().until(() -> processDetectionService.processFailed(conversationId));

        ehrStatus = retrieveEhrStatus();
        assertThat(ehrStatus.getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    private String fetchFirstCopcMessageId() {
        var ehrStatus = extractStatusRepository.findByConversationId(conversationId);
        var gpcDocument = ehrStatus
            .map(EhrExtractStatus::getGpcAccessDocument)
            .map(EhrExtractStatus.GpcAccessDocument::getDocuments)
            .flatMap(gpcDocuments -> gpcDocuments.stream().findFirst())
            .orElseThrow();

        return gpcDocument.getMessageId();
    }

    private void sendContinueToQueue() {
        var ebxml = asString(continueEbxml)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId);

        var payload = asString(continuePayload);

        sendInboundMessageToQueue(ebxml, payload);
    }

    private void sendRequestToQueue() {

        var ebxml = asString(requestEbxml)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId);

        var payload = asString(requestPayload)
            .replace(FROM_ASID_PLACEHOLDER, REQUESTOR_ASID)
            .replace(TO_ASID_PLACEHOLDER, SENDER_ASID);

        sendInboundMessageToQueue(ebxml, payload);
    }

    private void sendNackToQueue(String reasonCode, String reasonDisplay, String messageRef) {
        var messageId = UUID.randomUUID().toString().toUpperCase();

        var ebxml = asString(ackHeader)
            .replace(MESSAGE_ID_PLACEHOLDER, messageId)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId);

        var payload = asString(nackPayload)
            .replace(MESSAGE_ID_PLACEHOLDER, messageId)
            .replace(MESSAGE_REF_PLACEHOLDER, messageRef)
            .replace(TO_ASID_PLACEHOLDER, SENDER_ASID)
            .replace(FROM_ASID_PLACEHOLDER, REQUESTOR_ASID)
            .replace(REASON_CODE_PLACEHOLDER, reasonCode)
            .replace(REASON_DISPLAY_PLACEHOLDER, reasonDisplay);

        sendInboundMessageToQueue(ebxml, payload);
    }

    private void sendInboundMessageToQueue(String ebxml, String payload) {
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML(ebxml);
        inboundMessage.setPayload(payload);

        inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));
    }

    @SneakyThrows
    private String parseMessageToString(InboundMessage inboundMessage) {
        return objectMapper.writeValueAsString(inboundMessage);
    }

    private EhrStatus retrieveEhrStatus() {
        return restTemplate.getForObject(ehrStatusEndpoint, EhrStatus.class);
    }
}
