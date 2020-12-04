package uk.nhs.adaptors.mockmhsservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final JmsTemplate jmsTemplate;

    @Value("${gp2gp.amqp.taskQueueName}")
    private String mhsTaskQueueName;

    public void handleRequest(String json) {
        jmsTemplate.send(mhsTaskQueueName, session -> session.createTextMessage(json));
    }
}