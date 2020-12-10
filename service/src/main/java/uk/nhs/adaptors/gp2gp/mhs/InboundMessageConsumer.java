package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrRequestHandler;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageConsumer {
    private final ObjectMapper objectMapper;
    private final EhrRequestHandler ehrRequestHandler;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    public void receive(Message message) throws IOException, JMSException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received new message {}", messageID);
        try {
            String body = JmsReader.readMessage(message);
            LOGGER.debug("Message {} content: {}", messageID, body);
            var mhsInboundMessage = objectMapper.readValue(body, InboundMessage.class);
            // TODO: NIAD-776 if the inbound message is an EhrRequest interaction then use the ehrRequestHandler
            ehrRequestHandler.handleRequest(objectMapper.writeValueAsString(mhsInboundMessage));
            // TODO: NIAD-776 else we don't know how to handle the message, this is an error
            message.acknowledge();
            LOGGER.info("Acknowledged message {}", messageID);
        } catch (Exception e) {
            LOGGER.error("Error while processing MHS inbound queue message {}", messageID, e);
            throw e; //message will be sent to DLQ after few unsuccessful redeliveries
        }
    }
}
