package uk.nhs.adaptors.gp2gp.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.models.MhsInboundMessage;
import uk.nhs.adaptors.gp2gp.services.GP2GPService;
import uk.nhs.adaptors.gp2gp.utils.JmsReader;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MHSInboundMessageConsumer {

    private final ObjectMapper objectMapper;
    private final GP2GPService gp2GPService;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    public void receive(Message message) throws IOException, JMSException, XPathExpressionException, SAXException, ParserConfigurationException {
        LOGGER.info("Received new message {}", message.getJMSMessageID());
        LOGGER.debug("Received message: {}", message);
        try {
            String body = JmsReader.readMessage(message);
            LOGGER.debug("Received message body: {}", body);
            var mhsInboundMessage = objectMapper.readValue(body, MhsInboundMessage.class);
            LOGGER.debug("Decoded message: {}", mhsInboundMessage);
            gp2GPService.handleRequest(mhsInboundMessage.getPayload());
            message.acknowledge();
            LOGGER.info("Message acknowledged");
        } catch (Exception e) {
            LOGGER.error("Error while processing MHS inbound queue message", e);
            throw e; //message will be sent to DLQ after few unsuccessful redeliveries
        }
    }
}
