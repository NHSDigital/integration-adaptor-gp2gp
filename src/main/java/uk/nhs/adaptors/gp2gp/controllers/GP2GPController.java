package uk.nhs.adaptors.gp2gp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GP2GPController {

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private final JmsTemplate jmsTemplate;

    @Value("${gp2gp.amqp.inboundQueueName}")
    protected String mhsInboundQueueName;

    @PostMapping(path = "/structuredRecord", consumes = JSON_CONTENT_TYPE)
    public ResponseEntity<?> getStructuredRecord(@RequestBody() String json) {

        jmsTemplate.send(mhsInboundQueueName, session -> session.createTextMessage(json));

        return ResponseEntity.accepted().build();
    }
}
