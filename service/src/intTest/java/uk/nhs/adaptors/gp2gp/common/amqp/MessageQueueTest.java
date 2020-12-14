package uk.nhs.adaptors.gp2gp.common.amqp;

import static java.lang.Thread.sleep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

import javax.jms.Message;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.constants.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageHandler;
import uk.nhs.adaptors.gp2gp.repositories.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
@DirtiesContext
public class MessageQueueTest {
    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final long TIMEOUT = 5000L;
    private static final long EXPECTED_COUNT = 1;

    @Autowired
    private JmsTemplate jmsTemplate;
    @Value("${gp2gp.amqp.inboundQueueName}")
    private String inboundQueueName;
    @MockBean // mock the message handler to prevent any forward processing by the application
    private InboundMessageHandler inboundMessageHandler;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_SendingValidMessage_Expect_InboundMessageHandlerCallWithSameMessage() throws Exception {
        var inboundMessage = new InboundMessage();
        inboundMessage.setPayload("payload");
        var sentMessageContent = objectMapper.writeValueAsString(inboundMessage);
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(sentMessageContent));

        verify(inboundMessageHandler, timeout(TIMEOUT)).handle(argThat(jmsMessage ->
                hasSameContentAsSentMessage(jmsMessage, sentMessageContent)
        ));
    }

    @Test
    public void When_SendingValidMessage_Expect_MessageIsAddedToDB() throws IOException, InterruptedException {
        String ehtExtractRequestBody = IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(ehtExtractRequestBody));

        sleep(TIMEOUT);

        Iterator<EhrExtractStatus> ehrExtractStatusIterator = ehrExtractStatusRepository.findAll().iterator();
        assertThat(ehrExtractStatusIterator.hasNext(), is(true));

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusIterator.next();
        assertThat(ehrExtractStatus.getExtractId(), is(notNullValue()));
        assertThat(ehrExtractStatus.getConversationId(), is(notNullValue()));
        assertThat(ehrExtractStatus.getConversationId(), is(notNullValue()));
        assertThat(ehrExtractStatus.getConversationId(), is(EhrStatusConstants.CONVERSATION_ID));

        EhrExtractStatus.EhrRequest ehrRequest = ehrExtractStatus.getEhrRequest();

        assertThat(ehrRequest, is(notNullValue()));
        assertThat(ehrRequest.getRequestId(), is(EhrStatusConstants.REQUEST_ID));
        assertThat(ehrRequest.getNhsNumber(), is(EhrStatusConstants.NHS_NUMBER));
        assertThat(ehrRequest.getFromPartyId(), is(EhrStatusConstants.FROM_PARTY_ID));
        assertThat(ehrRequest.getToPartyId(), is(EhrStatusConstants.TO_PARTY_ID));
        assertThat(ehrRequest.getFromAsid(), is(EhrStatusConstants.FROM_ASID));
        assertThat(ehrRequest.getToAsid(), is(EhrStatusConstants.TO_ASID));
        assertThat(ehrRequest.getFromOdsCode(), is(EhrStatusConstants.FROM_ODS_CODE));
        assertThat(ehrRequest.getToOdsCode(), is(EhrStatusConstants.TO_ODS_CODE));
    }

    @Test
    public void When_SendingTwoValidMessageWithTheSameConversationIdAndRequestId_Expect_ExtraMessageIsNotAddedToDb()
        throws IOException, InterruptedException {
        String ehtExtractRequestBody = IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(ehtExtractRequestBody));

        sleep(TIMEOUT);

        ehrExtractStatusRepository.count();
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(ehtExtractRequestBody));

        sleep(TIMEOUT);

        long count = ehrExtractStatusRepository.count();

        assertThat(count, is(EXPECTED_COUNT));
    }

    @SneakyThrows
    public boolean hasSameContentAsSentMessage(Message receivedMessage, String sentMessageContent) {
        var actualMessageText = JmsReader.readMessage(receivedMessage);
        return sentMessageContent.equals(actualMessageText);
    }
}
