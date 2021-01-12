package uk.nhs.adaptors.mockmhsservice.service;

import javax.jms.JMSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final InboundProducer inboundProducer;

    public void handleRequest(String json) throws JMSException {
        inboundProducer.sendToMhsInboundQueue(json);
    }
}
