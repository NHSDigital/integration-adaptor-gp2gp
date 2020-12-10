package uk.nhs.adaptors.gp2gp.common.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageHandler;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import javax.jms.Message;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
public class MessageQueueTest {
    private static final long WAIT_TIME = 5000L;

    @Autowired
    private JmsTemplate jmsTemplate;
    @Value("${gp2gp.amqp.inboundQueueName}")
    private String inboundQueueName;
    @MockBean // mock the message handler to prevent any forward processing by the application
    private InboundMessageHandler inboundMessageHandler;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void When_SendingValidMessage_Expect_InboundMessageHandlerCallWithSameMessage() throws Exception {
        var inboundMessage = new InboundMessage();
        inboundMessage.setPayload("payload");
        var sentMessageContent = objectMapper.writeValueAsString(inboundMessage);
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(sentMessageContent));

        Thread.sleep(WAIT_TIME);
        verify(inboundMessageHandler).handle(argThat(jmsMessage ->
                hasSameContentAsSentMessage(jmsMessage, sentMessageContent)
        ));
    }

    @SneakyThrows
    public boolean hasSameContentAsSentMessage(Message receivedMessage, String sentMessageContent) {
        var actualMessageText = JmsReader.readMessage(receivedMessage);
        return sentMessageContent.equals(actualMessageText);
    }
}
