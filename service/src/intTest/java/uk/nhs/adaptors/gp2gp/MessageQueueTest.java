package uk.nhs.adaptors.gp2gp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.containers.MessageQueueExtension;
import uk.nhs.adaptors.gp2gp.configurations.AmqpProperties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith({SpringExtension.class, MessageQueueExtension.class})
@AutoConfigureMockMvc
@DirtiesContext
@Slf4j
public class MessageQueueTest {

    private static final String MESSAGE = "{\"payload\":\"myTestPayload\"}";

    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private AmqpProperties amqpProperties;


    @Test
    public void When_ConsumingInboundQueueMessage_Expect_PublishToTaskQueue() throws JMSException {

        jmsTemplate.send("inbound", session -> {
            TextMessage message = session.createTextMessage(MESSAGE);
            return message;
        });

        Message jmsMessage = jmsTemplate.receive("taskQueue");
        if (jmsMessage == null) {
            throw new IllegalStateException("Message must not be null");
        }

        String messageBody = jmsMessage.getBody(String.class);
        assertThat(messageBody).isEqualTo(MESSAGE);
    }
}
