package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;

import javax.jms.JMSException;
import javax.jms.Message;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageConsumer {
    private final InboundMessageHandler inboundMessageHandler;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    public void receive(Message message) throws JMSException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received new message {}", messageID);

        LOGGER.info("Received inbound message {}", messageID);
        try {
            String body = JmsReader.readMessage(message);
            LOGGER.debug("Message {} content: {}", messageID, body);

            inboundMessageHandler.handle(message);
            message.acknowledge();

            LOGGER.info("Acknowledged message {}", messageID);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while processing MHS inbound queue message {}", messageID, e);
            throw new InvalidInboundMessageException(e.getMessage());
        }
    }
}
