package uk.nhs.adaptors.gp2gp;

import static org.assertj.core.api.Assertions.assertThat;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import uk.nhs.adaptors.gp2gp.extension.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.extension.MongoDBExtension;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
public class MessageQueueTest {
    private static final String INVALID_MESSAGE_CONTENT = "TRASH";
    private static final String DLQ = "ActiveMQ.DLQ";

    @Autowired
    private JmsTemplate jmsTemplate;
    @Value("${gp2gp.amqp.inboundQueueName}")
    private String inboundQueueName;

    @Test
    public void When_SendingInvalidMessage_Expect_MessageIsSentToDeadLetterQueue() throws JMSException {
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(INVALID_MESSAGE_CONTENT));
        Message message = jmsTemplate.receive(DLQ);
        assert message != null;
        assertThat(message.getBody(String.class)).isEqualTo(INVALID_MESSAGE_CONTENT);
    }
}
