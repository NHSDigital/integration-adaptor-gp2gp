package uk.nhs.adaptors.mockmhsservice.service;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.JmsException;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.mockmhsservice.common.MockMHSException;
import uk.nhs.adaptors.mockmhsservice.common.OutboundMessage;
import uk.nhs.adaptors.mockmhsservice.common.ResourceReader;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {
    private static final String EXTRACT_CORE_INTERACTION_ID = "RCMR_IN030000UK06";
    private static final String ACKNOWLEDGEMENT_INTERACTION_ID = "MCCI_IN010000UK13";
    private static final String COMMON_INTERACTION_ID = "COPC_IN000001UK01";
    private static final String STUB_CONTINUE_REPLY_INBOUND_MESSAGE = ResourceReader.readAsString("COPC_IN000001UK01.json");
    private static final String STUB_ACCEPTED_RESPONSE = ResourceReader.readAsString("StubEbXmlResponse.xml");
    private static final String INTERNAL_SERVER_ERROR_RESPONSE = ResourceReader.readAsString("InternalServerError.html");
    private static final String STUB_ACKNOWLEDGEMENT_INBOUND_MESSAGE = ResourceReader.readAsString("MCCI_IN010000UK13.json");

    private static final Map<String, String> EHR_EXTRACT_ID_MAP = new ConcurrentHashMap<>();

    private final InboundProducer inboundProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpHeaders headers = new HttpHeaders();

    public ResponseEntity<String> handleRequest(String interactionId, String correlationId, String waitForResponse, String mockMhsMessage,
        String odsCode, String messageId) {
        headers.setContentType(MediaType.TEXT_HTML);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        if (odsCode.isEmpty()) {
            LOGGER.error("Missing ods-code header");
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, HttpStatus.BAD_REQUEST);
        }

        if (!waitForResponse.equals("false")) {
            LOGGER.error("Missing or invalid wait-for-response header");
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, HttpStatus.BAD_REQUEST);
        }

        try {
            verifyOutboundMessagePayload(mockMhsMessage);
        } catch (MockMHSException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error content of request body does not match expected JSON", e);
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals(EXTRACT_CORE_INTERACTION_ID)) {
            try {
                EHR_EXTRACT_ID_MAP.put(correlationId, messageId);
                var inboundMessage = STUB_CONTINUE_REPLY_INBOUND_MESSAGE.replace("%%ConversationId%%", correlationId);
                inboundProducer.sendToMhsInboundQueue(inboundMessage);
                LOGGER.info("Placed message on Inbound Queue, conversationId: {}", correlationId);
                headers.setContentType(MediaType.TEXT_XML);
                return new ResponseEntity<>(STUB_ACCEPTED_RESPONSE, headers, ACCEPTED);
            } catch (JmsException e) {
                LOGGER.error("Error could not produce inbound reply", e);
                return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
            }
        } else if (interactionId.equals(ACKNOWLEDGEMENT_INTERACTION_ID)) {
            LOGGER.info("Message acknowledgement accepted.");
            headers.setContentType(MediaType.TEXT_XML);
            try {
                Optional<String> ehrExtractMessageId = Optional.ofNullable(EHR_EXTRACT_ID_MAP.get(correlationId));
                var inboundMessage = STUB_ACKNOWLEDGEMENT_INBOUND_MESSAGE.replace("%%ConversationId%%", correlationId);

                if(ehrExtractMessageId.isPresent()) {
                    inboundMessage = inboundMessage.replace("%%messageRef%%", ehrExtractMessageId.get());
                }

                inboundProducer.sendToMhsInboundQueue(inboundMessage);
                LOGGER.info("Message acknowledgement sent to Inbound Queue, conversationId: {}", correlationId);
                headers.setContentType(MediaType.TEXT_XML);
                return new ResponseEntity<>(STUB_ACCEPTED_RESPONSE, headers, ACCEPTED);
            } catch (JmsException e) {
                LOGGER.error("Error could not send acknowledgement to Inbound Queue", e);
                return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
            }
        } else if (interactionId.equals(COMMON_INTERACTION_ID)) {
            LOGGER.info("Message Common accepted.");
            headers.setContentType(MediaType.TEXT_XML);
            return new ResponseEntity<>(headers, ACCEPTED);
        }
        LOGGER.error("Error could not handle request header Interaction-Id: {}", interactionId);
        return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<String> accept() {
        return new ResponseEntity<>(STUB_ACCEPTED_RESPONSE, headers, ACCEPTED);
    }

    private void verifyOutboundMessagePayload(String requestBody) throws JsonProcessingException, MockMHSException {
        LOGGER.debug("Received outbound MHS request payload:\n{}", requestBody);
        var payloadObject = objectMapper.readValue(requestBody, OutboundMessage.class);
        if (payloadObject.getPayload() == null) {
            throw new MockMHSException("Error content of request body does not match expected JSON");
        }
    }
}
