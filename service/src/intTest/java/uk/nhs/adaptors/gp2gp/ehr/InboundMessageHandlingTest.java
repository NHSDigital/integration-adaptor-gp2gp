package uk.nhs.adaptors.gp2gp.ehr;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class InboundMessageHandlingTest {
    private static final String CONVERSATION_ID_PLACEHOLDER = "{{conversationId}}";
    private static final String EBXML_PATH = "/continuemessage/COPC_IN000001UK01_ebxml.txt";
    private static final String PAYLOAD_PATH = "/continuemessage/COPC_IN000001UK01_payload.txt";
    private static final String INCORRECT_PAYLOAD_PATH = "/continuemessage/COPC_IN000001UK01_incorrect_payload.txt";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private JmsTemplate inboundJmsTemplate;

    private String conversationId;

    @BeforeEach
    public void setUp() {
        inboundJmsTemplate.setDefaultDestinationName("inbound");
        conversationId = UUID.randomUUID().toString();
    }

    @Test
    public void When_MessageIsUnreadable_Expect_MessageProcessingToBeAborted() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(conversationId);
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);
        sendUnreadableInboundMessageToQueue();

//        verify(message, never()).acknowledge(); // TODO
//        verifyNoInteractions(taskDispatcher); // TODO
        await().until(this::conversationIsFailed); // warunek nigdy nie jest spelniony, wydaje mi sie ze to dlatego, ze obsluga bledow w gp2gp jest kiepska
    }

    @Test
    public void When_MessageProcessingFails_Expect_WholeProcessToBeFailed() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(conversationId);
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        sendInboundMessageToQueue(INCORRECT_PAYLOAD_PATH);

//        verify(message).acknowledge(); // TODO
//        assertThatSendNackTaskHasBeenTriggered(); // TODO
        await().until(this::conversationIsFailed); // warunek nigdy nie jest spelniony, wydaje mi sie ze to dlatego, ze obsluga bledow w gp2gp jest kiepska
    }

    @Test
    public void When_ProcessIsAlreadyFailed_Expect_MessageProcessingToBeAborted() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(conversationId);
        ehrExtractStatus.setError(EhrExtractStatus.Error.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var initialDbExtract = readEhrExtractStatusFromDb();

        sendInboundMessageToQueue(PAYLOAD_PATH);

//        verify(message).acknowledge(); // TODO
//        verifyNoInteractions(taskDispatcher); // TODO

        waitThreeSeconds();
        var finalDbExtract = readEhrExtractStatusFromDb();
        assertThat(finalDbExtract).usingRecursiveComparison().isEqualTo(initialDbExtract);
    }

    @Test
    public void When_ProcessIsNotFailed_Expect_MessageToBeProcessed() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(conversationId);
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var initialDbExtract = readEhrExtractStatusFromDb();
        assertThat(initialDbExtract.getEhrContinue()).isNull();

        sendInboundMessageToQueue(PAYLOAD_PATH);

//        verify(message).acknowledge(); // TODO
//        assertSendDocumentTaskHasBeenTriggered(); // TODO

        await().until(this::ehrContinueIsNotNull);
        assertConversationIsNotFailed();
    }

    private boolean ehrContinueIsNotNull() {
        var finalDbExtract = readEhrExtractStatusFromDb();
        return finalDbExtract.getEhrContinue() != null;
    }

    private boolean conversationIsFailed() {
        var dbExtract = readEhrExtractStatusFromDb();
        return dbExtract.getError() != null;
    }

    private void assertConversationIsNotFailed() {
        var dbExtract = readEhrExtractStatusFromDb();
        assertThat(dbExtract.getError()).isNull();
    }

    private EhrExtractStatus readEhrExtractStatusFromDb() {
        return ehrExtractStatusRepository.findByConversationId(conversationId).get();
    }

    private void sendInboundMessageToQueue(String payloadPartPath) {
        var inboundMessage = createInboundMessage(payloadPartPath);
        inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));
    }

    private InboundMessage createInboundMessage(String payloadPartPath) {
        var inboundMessage = new InboundMessage();
        var payload = readResourceAsString(payloadPartPath);
        var ebXml = readResourceAsString(EBXML_PATH).replace(CONVERSATION_ID_PLACEHOLDER, conversationId);
        inboundMessage.setPayload(payload);
        inboundMessage.setEbXML(ebXml);
        return inboundMessage;
    }

    private void sendUnreadableInboundMessageToQueue() {
        inboundJmsTemplate.send(session -> session.createTextMessage("not a json"));
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

    @SneakyThrows
    private void waitThreeSeconds() {
        TimeUnit.SECONDS.sleep(3);
    }
}
