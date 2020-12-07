package uk.nhs.adaptors.mockmhsservice.service;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final JmsTemplate jmsTemplate;

    @Value("${gp2gp.amqp.inboundQueueName}")
    private String mhsTaskQueueName;

    public String handleRequest(String json) throws JMSException {
        jmsTemplate.send(mhsTaskQueueName, session -> session.createTextMessage(json));

        Message jmsMessage = jmsTemplate.receive("taskQueue");
        if (jmsMessage == null) {
            throw new IllegalStateException("Message must not be null");
        }

        return jmsMessage.getBody(String.class);
    }
}