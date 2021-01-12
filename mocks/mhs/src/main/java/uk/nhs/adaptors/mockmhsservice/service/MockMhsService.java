package uk.nhs.adaptors.mockmhsservice.service;

import javax.jms.JMSException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import uk.nhs.adaptors.mockmhsservice.message.InboundMessage;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final ObjectMapper objectMapper;
    private final InboundProducer inboundProducer;

    public ResponseEntity<String> handleRequest(String interactionId, String json) throws JMSException {
        String mockSuccessMessage = "{\"message\": \"Message acknowledged.\"}";
        String mockErrorMessage = "{\"message\": \"Error, cannot handle request header Interaction-Id.\"}";

        try {
            verifyJson(json);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals("RCMR_IN030000UK06")) {
            inboundProducer.sendToMhsInboundQueue(json);
            return new ResponseEntity<>(mockSuccessMessage, ACCEPTED);
        }

        return new ResponseEntity<>(mockErrorMessage, INTERNAL_SERVER_ERROR);
    }

    private InboundMessage verifyJson(String json) throws Exception {
        try {
            return objectMapper.readValue(json, InboundMessage.class);
        } catch (JsonProcessingException e) {
            throw new Exception("Content of the inbound MHS message is not valid JSON", e);
        }
    }
}
