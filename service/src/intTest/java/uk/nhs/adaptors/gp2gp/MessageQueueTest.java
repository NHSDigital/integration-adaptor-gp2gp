package uk.nhs.adaptors.gp2gp;

import static org.assertj.core.api.Assertions.assertThat;

import javax.jms.JMSException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import uk.nhs.adaptors.gp2gp.configurations.AmqpProperties;
import uk.nhs.adaptors.gp2gp.extension.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.extension.IntegrationTestsExtension;

@SpringBootTest
@ExtendWith({IntegrationTestsExtension.class, ActiveMQExtension.class})
public class MessageQueueTest {

    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private AmqpProperties amqpProperties;

    private static final String MESSAGE_CONTENT = "TRASH";

    @Test
    public void whenSendingInvalidMessage_thenMessageIsSentToDeadLetterQueue() throws JMSException {
        jmsTemplate.send("inbound", session -> session.createTextMessage(MESSAGE_CONTENT));
        String messageBody = jmsTemplate.receive("ActiveMQ.DLQ").getBody(String.class);

        assertThat(messageBody).isEqualTo(MESSAGE_CONTENT);
    }
}
