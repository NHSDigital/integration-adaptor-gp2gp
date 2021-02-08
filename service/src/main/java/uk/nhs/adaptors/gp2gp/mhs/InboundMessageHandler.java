package uk.nhs.adaptors.gp2gp.mhs;

import static uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction.CONTINUE_REQUEST;
import static uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction.EHR_EXTRACT_REQUEST;

import javax.jms.JMSException;
import javax.jms.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageHandler {
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";
    private static final String CONVERSATION_ID_PATH = "/Envelope/Header/MessageHeader/ConversationId";

    private final ObjectMapper objectMapper;
    private final EhrExtractRequestHandler ehrExtractRequestHandler;
    private final XPathService xPathService;
    private final MDCService mdcService;

    public void handle(Message message) {
        var inboundMessage = unmarshallMessage(message);
        LOGGER.info("Decoded in inbound MHS message");
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
        final Document ebXmlDocument = getMessageEnvelope(inboundMessage);
        final Document payloadDocument = getMessagePayload(inboundMessage);

        var conversationId = getConversationId(ebXmlDocument);
        mdcService.applyConversationId(conversationId);

        var interactionId = getInteractionId(ebXmlDocument);
        LOGGER.info("The inbound MHS message uses interaction id {}", interactionId);

        if (EHR_EXTRACT_REQUEST.getInteractionId().equals(interactionId)) {
            ehrExtractRequestHandler.handleStart(ebXmlDocument, payloadDocument);
        } else if (CONTINUE_REQUEST.getInteractionId().equals(interactionId)) {
            ehrExtractRequestHandler.handleContinue(conversationId, inboundMessage.getPayload());
        } else {
            throw new UnsupportedInteractionException(interactionId);
        }
    }

    private Document getMessageEnvelope(InboundMessage inboundMessage) {
        try {
            return xPathService.parseDocumentFromXml(inboundMessage.getEbXML());
        } catch (SAXException e) {
            throw new InvalidInboundMessageException("Unable to parse the XML envelope (ebxml) of the inbound MHS message", e);
        }
    }

    private Document getMessagePayload(InboundMessage inboundMessage) {
        try {
            return xPathService.parseDocumentFromXml(inboundMessage.getPayload());
        } catch (SAXException e) {
            throw new InvalidInboundMessageException("Unable to parse the XML payload of the inbound MHS message", e);
        }
    }

    private String getInteractionId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, ACTION_PATH);
    }

    private String getConversationId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, CONVERSATION_ID_PATH);
    }
}
