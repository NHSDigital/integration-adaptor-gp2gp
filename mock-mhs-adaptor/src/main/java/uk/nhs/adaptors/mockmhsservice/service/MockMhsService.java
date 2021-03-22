package uk.nhs.adaptors.mockmhsservice.service;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.mockmhsservice.common.MockMHSException;
import uk.nhs.adaptors.mockmhsservice.common.OutboundMessage;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {
    private final InboundProducer inboundProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpHeaders headers = new HttpHeaders();

    private static final String EXTRACT_CORE_INTERACTION_ID = "RCMR_IN030000UK06";
    private static final String ACKNOWLEDGEMENT_INTERACTION_ID = "MCCI_IN010000UK13";
    private static final ClassLoader CLASS_LOADER = MockMhsService.class.getClassLoader();
    private static final InputStream INPUT_STREAM_CONTINUE_REPLY = CLASS_LOADER.getResourceAsStream("COPC_IN000001UK01.json");
    private static final String STUB_CONTINUE_REPLY_INBOUND_MESSAGE = IOUtils.toString(INPUT_STREAM_CONTINUE_REPLY, StandardCharsets.UTF_8);
    private static final InputStream INPUT_STREAM_ACCEPTED_RESPONSE = CLASS_LOADER.getResourceAsStream("StubEbXmlResponse.xml");
    private static final String STUB_ACCEPTED_RESPONSE = IOUtils.toString(INPUT_STREAM_ACCEPTED_RESPONSE, StandardCharsets.UTF_8);
    private static final InputStream INPUT_STREAM_INTERNAL_SERVER_ERROR = CLASS_LOADER.getResourceAsStream("InternalServerError.html");
    private static final String INTERNAL_SERVER_ERROR_RESPONSE = IOUtils.toString(INPUT_STREAM_INTERNAL_SERVER_ERROR, StandardCharsets.UTF_8);

    public ResponseEntity<String> handleRequest(String interactionId, String correlationId, String waitForResponse, String mockMhsMessage,
        String odsCode) {
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
                var inboundMessage = STUB_CONTINUE_REPLY_INBOUND_MESSAGE.replace("%%ConversationId%%", correlationId);
                inboundProducer.sendToMhsInboundQueue(inboundMessage);
                LOGGER.info("Placed message on Inbound Queue, conversationId: " + correlationId);
                headers.setContentType(MediaType.TEXT_XML);
                return new ResponseEntity<>(STUB_ACCEPTED_RESPONSE, headers, ACCEPTED);
            } catch (JmsException e) {
                LOGGER.error("Error could not produce inbound reply", e);
                return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
            }
        } else if (interactionId.equals(ACKNOWLEDGEMENT_INTERACTION_ID)) {
            LOGGER.info(String.format("Message acknowledgement accepted, conversationId: %s", correlationId));
            headers.setContentType(MediaType.TEXT_XML);
            return new ResponseEntity<>(STUB_ACCEPTED_RESPONSE, headers, ACCEPTED);
        }

        LOGGER.error("Error could not handle request header Interaction-Id {}", interactionId);
        return new ResponseEntity<>(INTERNAL_SERVER_ERROR_RESPONSE, headers, INTERNAL_SERVER_ERROR);
    }

    private void verifyOutboundMessagePayload(String requestBody) throws JsonProcessingException, MockMHSException {
        var payloadObject = objectMapper.readValue(requestBody, OutboundMessage.class);
        if (payloadObject.getPayload() == null) {
            throw new MockMHSException("Error content of request body does not match expected JSON");
        }
    }
}
