package uk.nhs.adaptors.mockmhsservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.JmsException;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import uk.nhs.adaptors.mockmhsservice.common.OutboundMessage;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final InboundProducer inboundProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String mockValidInteractionId = "RCMR_IN030000UK06";

    private final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("COPC_IN000001UK01.json");
    private final String stubInboundMessage = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

    public ResponseEntity<String> handleRequest(String interactionId, String mockMhsMessage) throws IOException {

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        try {
            verifyOutboundMessagePayload(mockMhsMessage);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error content of request body does not match expected JSON", e);
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals(mockValidInteractionId)) {
            try {
                inboundProducer.sendToMhsInboundQueue(stubInboundMessage);
                LOGGER.info("Placed stub message on Inbound Queue");
                return new ResponseEntity<>(ACCEPTED);
            } catch (JmsException e) {
                LOGGER.error("Error could not produce inbound reply", e);
                return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
            }
        }

        LOGGER.error("Error cannot handle request header Interaction-Id {}", interactionId);
        return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
    }

    private void verifyOutboundMessagePayload(String requestBody) throws JsonProcessingException {
        objectMapper.readValue(requestBody, OutboundMessage.class);
    }
}
