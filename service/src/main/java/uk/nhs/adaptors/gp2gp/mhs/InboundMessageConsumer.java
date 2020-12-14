package uk.nhs.adaptors.gp2gp.mhs;

import javax.jms.JMSException;
import javax.jms.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.task.EhrStatusConsumerService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrRequestHandler;
import uk.nhs.adaptors.gp2gp.exceptions.InvalidInboundMessageException;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageConsumer {
    private final InboundMessageHandler inboundMessageHandler;
    private static final String INTERACTION_ID = "RCMR_IN010000UK05";
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";

    private final ObjectMapper objectMapper;
    private final EhrRequestHandler ehrRequestHandler;
    private final EhrStatusConsumerService ehrStatusConsumerService;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    public void receive(Message message) throws JMSException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received new message {}", messageID);

        LOGGER.info("Received inbound message {}", messageID);
        try {
            String body = JmsReader.readMessage(message);
            LOGGER.debug("Message {} content: {}", messageID, body);

            handleMhsRequest(body);
            inboundMessageHandler.handle(message);
            message.acknowledge();

            LOGGER.info("Acknowledged message {}", messageID);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while processing MHS inbound queue message {}", messageID, e);
            throw new InvalidInboundMessageException(e.getMessage());
        }
    }

    private void handleMhsRequest(String body) throws JsonProcessingException {
        var mhsInboundMessage = objectMapper.readValue(body, InboundMessage.class);
        Document ebXmlDocument = XPathService.prepareDocumentFromXml(mhsInboundMessage.getEbXML());
        Document payloadDocument = XPathService.prepareDocumentFromXml(mhsInboundMessage.getPayload());

        if (isEhrStatusRequest(ebXmlDocument)) {
            ehrStatusConsumerService.handleEhrStatus(ebXmlDocument, payloadDocument);
        } else {
            ehrRequestHandler.handleRequest(objectMapper.writeValueAsString(mhsInboundMessage));
        }
    }

    private boolean isEhrStatusRequest(Document ebXmlDocument) {
        return XPathService.getTagValue(ebXmlDocument, ACTION_PATH).equals(INTERACTION_ID);
    }
}
