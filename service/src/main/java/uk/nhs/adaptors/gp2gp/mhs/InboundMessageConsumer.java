package uk.nhs.adaptors.gp2gp.mhs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageConsumer {

    private final InboundMessageHandler inboundMessageHandler;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    public void receive(Message message) throws IOException, JMSException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received inbound message {}", messageID);
        try {
            inboundMessageHandler.handle(message);
            message.acknowledge();
            LOGGER.info("Acknowledged message {}", messageID);
        } catch (Exception e) {
            LOGGER.error("Error while processing MHS inbound queue message {}", messageID, e);
            throw e; //message will be sent to DLQ after few unsuccessful redeliveries
        }
    }
}
