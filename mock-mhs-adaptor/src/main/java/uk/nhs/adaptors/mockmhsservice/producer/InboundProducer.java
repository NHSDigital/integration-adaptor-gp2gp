package uk.nhs.adaptors.mockmhsservice.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundProducer {

    private final JmsTemplate jmsTemplate;
    @Value("${gp2gp.amqp.inboundQueueName}")
    private String queueName;

    public void sendToMhsInboundQueue(String messageContent) throws JmsException {
        jmsTemplate.send(queueName, session -> session.createTextMessage(messageContent));
    }
}
