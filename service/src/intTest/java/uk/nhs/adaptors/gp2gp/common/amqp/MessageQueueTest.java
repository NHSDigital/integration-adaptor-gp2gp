package uk.nhs.adaptors.gp2gp.common.amqp;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import javax.jms.Message;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageHandler;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

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
    private static final String SOAP_HEADER = "<soap:Header></soap:Header>";
    private static final long TIMEOUT = 5000L;

    @Value("${gp2gp.amqp.inboundQueueName}")
    private String inboundQueueName;
    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @MockBean // mock the message handler to prevent any forward processing by the application
    private InboundMessageHandler inboundMessageHandler;

    @Test
    public void When_SendingValidMessage_Expect_InboundMessageHandlerCallWithSameMessage() throws Exception {
        var inboundMessage = new InboundMessage();
        inboundMessage.setPayload(SOAP_HEADER);
        inboundMessage.setEbXML(SOAP_HEADER);

        var sentMessageContent = objectMapper.writeValueAsString(inboundMessage);
        jmsTemplate.send(inboundQueueName, session -> session.createTextMessage(sentMessageContent));

        verify(inboundMessageHandler, timeout(TIMEOUT)).handle(argThat(jmsMessage ->
            hasSameContentAsSentMessage(jmsMessage, sentMessageContent)
        ));
    }

    @SneakyThrows
    public boolean hasSameContentAsSentMessage(Message receivedMessage, String sentMessageContent) {
        var actualMessageText = JmsReader.readMessage(receivedMessage);
        return sentMessageContent.equals(actualMessageText);
    }
}
