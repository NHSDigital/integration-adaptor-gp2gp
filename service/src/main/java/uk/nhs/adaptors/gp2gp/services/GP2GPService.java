package uk.nhs.adaptors.gp2gp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GP2GPService {

    private final JmsTemplate jmsTemplate;

    @Value("${gp2gp.amqp.outboundQueueName}")
    protected String mhsOutboundQueueName;

    public void handleRequest(String xml) {
        jmsTemplate.send(mhsOutboundQueueName, session -> session.createTextMessage(xml));
    }
}
