package uk.nhs.adaptors.mockmhsservice.service;

import javax.jms.JMSException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public ResponseEntity<String> handleRequest(String interactionId, String json) throws JMSException, JsonProcessingException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true); // should this be set to false?
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        ObjectNode rootNode = objectMapper.createObjectNode();

        String jsonString;
        String mockSuccessMessage = "Message acknowledged.";
        String mockErrorMessage = "Error, cannot handle request header Interaction-Id.";

        try {
            verifyJson(json);
        } catch (Exception e) {
            rootNode.put("message", e.getMessage());
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            return new ResponseEntity<>(jsonString, INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals("RCMR_IN030000UK06")) {
            inboundProducer.sendToMhsInboundQueue(json);
            rootNode.put("message", mockSuccessMessage);
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            return new ResponseEntity<>(jsonString, ACCEPTED);
        }

        rootNode.put("message", mockErrorMessage);
        jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        return new ResponseEntity<>(jsonString, INTERNAL_SERVER_ERROR);
    }

    private void verifyJson(String json) throws Exception {
        try {
            objectMapper.readValue(json, InboundMessage.class);
        } catch (JsonProcessingException e) {
            throw new Exception("Content of the inbound MHS message does not match expected JSON", e);
        }
    }
}
