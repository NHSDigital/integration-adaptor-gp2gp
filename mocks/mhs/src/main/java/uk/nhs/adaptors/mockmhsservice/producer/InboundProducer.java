package uk.nhs.adaptors.mockmhsservice.producer;

import javax.jms.JMSException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.mockmhsservice.common.MockMHSException;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundProducer {

    private final JmsTemplate jmsTemplate;
    private final String queueName = System.getenv().getOrDefault("GP2GP_MHS_INBOUND_QUEUE", "inbound");

    public void sendToMhsInboundQueue(String messageContent) throws MockMHSException {
        try {
            jmsTemplate.send(queueName, session -> session.createTextMessage(messageContent));
        } catch (MockMHSException e) {
            throw new MockMHSException("Error, could not produce inbound reply.", e);
        }
    }
}
