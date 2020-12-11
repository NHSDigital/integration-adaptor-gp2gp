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
    private static final String CONVERSATION_ID = "DFF5321C-C6EA-468E-BBC2-B0E48000E071";
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String NHS_NUMBER = "9692294935";
    private static final String FROM_PARTY_ID = "N82668-820670";
    private static final String TO_PARTY_ID = "B86041-822103";
    private static final String FROM_ASID = "200000000205";
    private static final String TO_ASID = "200000001161";
    private static final String FROM_ODS_CODE = "N82668";
    private static final String TO_ODS_CODE = "B86041";
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
        assertThat(ehrExtractStatus.getConversationId(), is(CONVERSATION_ID));

        EhrExtractStatus.EhrRequest ehrRequest = ehrExtractStatus.getEhrRequest();

        assertThat(ehrRequest, is(notNullValue()));
        assertThat(ehrRequest.getRequestId(), is(REQUEST_ID));
        assertThat(ehrRequest.getNhsNumber(), is(NHS_NUMBER));
        assertThat(ehrRequest.getFromPartyId(), is(FROM_PARTY_ID));
        assertThat(ehrRequest.getToPartyId(), is(TO_PARTY_ID));
        assertThat(ehrRequest.getFromAsid(), is(FROM_ASID));
        assertThat(ehrRequest.getToAsid(), is(TO_ASID));
        assertThat(ehrRequest.getFromOdsCode(), is(FROM_ODS_CODE));
        assertThat(ehrRequest.getToOdsCode(), is(TO_ODS_CODE));
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
