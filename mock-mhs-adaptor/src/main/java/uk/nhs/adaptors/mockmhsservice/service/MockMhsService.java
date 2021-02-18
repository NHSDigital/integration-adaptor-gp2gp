package uk.nhs.adaptors.mockmhsservice.service;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
    private final String mockValidInteractionId = "RCMR_IN030000UK06";

    private final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("COPC_IN000001UK01.json");
    private final String stubInboundMessage = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    private final InputStream inputStream2 = this.getClass().getClassLoader().getResourceAsStream("StubEbXmlResponse.xml");
    private final String stubEbXmlResponse = IOUtils.toString(inputStream2, StandardCharsets.UTF_8);
    private final InputStream inputStream3 = this.getClass().getClassLoader().getResourceAsStream("InternalServerError.html");
    private final String internalServerErrorResponse = IOUtils.toString(inputStream3, StandardCharsets.UTF_8);

    public ResponseEntity<String> handleRequest(String interactionId, String correlationId, String waitForResponse, String mockMhsMessage,
        String contentType) {
        headers.setContentType(MediaType.TEXT_HTML);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        if (!contentType.equals(APPLICATION_JSON_VALUE)) {
            LOGGER.error("Missing or invalid content-type header");
            return new ResponseEntity<>(internalServerErrorResponse, headers, HttpStatus.BAD_REQUEST);
        }

        if (!waitForResponse.equals("false")) {
            LOGGER.error("Missing or invalid wait-for-response header");
            return new ResponseEntity<>(internalServerErrorResponse, headers, HttpStatus.BAD_REQUEST);
        }

        try {
            verifyOutboundMessagePayload(mockMhsMessage);
        } catch (MockMHSException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(internalServerErrorResponse, headers, INTERNAL_SERVER_ERROR);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error content of request body does not match expected JSON", e);
            return new ResponseEntity<>(internalServerErrorResponse, headers, INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals(mockValidInteractionId)) {
            try {
                var inboundMessage = stubInboundMessage.replace("%%ConversationId%%", correlationId);
                inboundProducer.sendToMhsInboundQueue(inboundMessage);
                LOGGER.info("Placed message on Inbound Queue, conversationId: " + correlationId);
                headers.setContentType(MediaType.TEXT_XML);
                return new ResponseEntity<>(stubEbXmlResponse, headers, ACCEPTED);
            } catch (JmsException e) {
                LOGGER.error("Error could not produce inbound reply", e);
                return new ResponseEntity<>(internalServerErrorResponse, headers, INTERNAL_SERVER_ERROR);
            }
        }

        LOGGER.error("Error could not handle request header Interaction-Id {}", interactionId);
        return new ResponseEntity<>(internalServerErrorResponse, headers, INTERNAL_SERVER_ERROR);
    }

    private void verifyOutboundMessagePayload(String requestBody) throws JsonProcessingException, MockMHSException {
        var payloadObject = objectMapper.readValue(requestBody, OutboundMessage.class);
        if (payloadObject.getPayload() == null) {
            throw new MockMHSException("Error content of request body does not match expected JSON");
        }
    }
}
