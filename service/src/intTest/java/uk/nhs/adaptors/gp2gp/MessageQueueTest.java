package uk.nhs.adaptors.gp2gp;

import static java.lang.Thread.sleep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

import javax.jms.JMSException;
import javax.jms.Message;

import uk.nhs.adaptors.gp2gp.constants.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.extension.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.extension.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.models.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.repositories.EhrExtractStatusRepository;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
public class MessageQueueTest {
    private static final String INVALID_MESSAGE_CONTENT = "TRASH";
    private static final String DLQ = "ActiveMQ.DLQ";
    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final long TIMEOUT = 2000;
    private static final long EXPECTED_COUNT = 1;

    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Value("${gp2gp.amqp.inboundQueueName}")
    private String inboundQueueName;

    @Test
    public void When_SendingInvalidMessage_Expect_MessageIsSentToDeadLetterQueue() throws JMSException {
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(INVALID_MESSAGE_CONTENT));
        Message message = jmsTemplate.receive(DLQ);
        assert message != null;
        assertThat(message.getBody(String.class), is(INVALID_MESSAGE_CONTENT));
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
}
