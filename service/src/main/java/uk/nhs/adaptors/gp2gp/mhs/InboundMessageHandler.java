package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.ehr.SpineInteraction;
import uk.nhs.adaptors.gp2gp.ehr.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;

import javax.jms.JMSException;
import javax.jms.Message;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageHandler {
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";

    private final ObjectMapper objectMapper;
    private final EhrExtractRequestHandler ehrExtractRequestHandler;
    private final XPathService xPathService;

    public void handle(Message message) {
        var inboundMessage = unmarshallMessage(message);
        handleInboundMessage(inboundMessage);
    }

    private InboundMessage unmarshallMessage(Message message) {
        try {
            var body = JmsReader.readMessage(message);
            return objectMapper.readValue(body, InboundMessage.class);
        } catch (JMSException e) {
            throw new InvalidInboundMessageException("Unable to read the content of the inbound MHS message", e);
        } catch (JsonProcessingException e) {
            throw new InvalidInboundMessageException("Content of the inbound MHS message is not valid JSON", e);
        }
    }

    private void handleInboundMessage(InboundMessage inboundMessage) {
        Document ebXmlDocument = xPathService.prepareDocumentFromXml(inboundMessage.getEbXML());
        Document payloadDocument = xPathService.prepareDocumentFromXml(inboundMessage.getPayload());
        var interactionId = getInteractionId(ebXmlDocument);

        if (SpineInteraction.EHR_EXTRACT_REQUEST.getInteractionId().equals(interactionId)) {
            ehrExtractRequestHandler.handle(ebXmlDocument, payloadDocument);
        } else {
            throw new UnsupportedInteractionException(interactionId);
        }
    }

    private String getInteractionId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, ACTION_PATH);
    }

}
