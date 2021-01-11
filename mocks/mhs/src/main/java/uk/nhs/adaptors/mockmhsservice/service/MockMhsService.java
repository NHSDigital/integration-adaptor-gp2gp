package uk.nhs.adaptors.mockmhsservice.service;

import javax.jms.JMSException;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    @SneakyThrows
    public String handleRequest(String json) throws JMSException {
        InboundProducer.sendToMhsInboundQueue(json);

        return "Message acknowledged";
    }
}