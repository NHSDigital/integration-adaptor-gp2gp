package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;
import uk.nhs.adaptors.gp2gp.utils.ConversationIdService;

import javax.jms.JMSException;
import javax.jms.Message;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageHandler {
    private static final String EHR_EXTRACT_REQUEST_INTERACTION_ID = "RCMR_IN010000UK05";
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";
    private static final String CONVERSATION_ID_PATH = "/Envelope/Header/MessageHeader/ConversationId";

    private final ObjectMapper objectMapper;
    private final EhrExtractRequestHandler ehrExtractRequestHandler;
    private final XPathService xPathService;
    private final ConversationIdService conversationIdService;

    public void handle(Message message) throws JMSException, JsonProcessingException {
        String body = JmsReader.readMessage(message);
        LOGGER.debug("Message content: {}", body);
        handleMhsRequest(body);
    }

    private void handleMhsRequest(String body) throws JsonProcessingException {
        var mhsInboundMessage = objectMapper.readValue(body, InboundMessage.class);
        Document ebXmlDocument = xPathService.prepareDocumentFromXml(mhsInboundMessage.getEbXML());
        Document payloadDocument = xPathService.prepareDocumentFromXml(mhsInboundMessage.getPayload());
        var interactionId = getInteractionId(ebXmlDocument);
        var conversationId = getConversationId(ebXmlDocument);

        conversationIdService.applyConversationId(conversationId);

        if (isEhrStatusRequest(interactionId)) {
            ehrExtractRequestHandler.handleEhrStatus(ebXmlDocument, payloadDocument);
        } else {
            throw new UnsupportedInteractionException(interactionId);
        }
    }

    private String getInteractionId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, ACTION_PATH);
    }

    private String getConversationId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, CONVERSATION_ID_PATH);
    }

    private boolean isEhrStatusRequest(String interactionId) {
        return interactionId.equals(EHR_EXTRACT_REQUEST_INTERACTION_ID);
    }
}
