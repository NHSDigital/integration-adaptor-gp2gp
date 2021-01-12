package uk.nhs.adaptors.mockmhsservice.producer;

import javax.jms.JMSException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundProducer {

    private final JmsTemplate jmsTemplate;
    private final String queueName = System.getenv().getOrDefault("GP2GP_MHS_INBOUND_QUEUE", "inbound");

    public void sendToMhsInboundQueue(String messageContent) throws JMSException {
        jmsTemplate.send(queueName, session -> session.createTextMessage(messageContent));
    }
}
