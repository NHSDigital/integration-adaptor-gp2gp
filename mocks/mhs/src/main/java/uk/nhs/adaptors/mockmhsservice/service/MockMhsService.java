package uk.nhs.adaptors.mockmhsservice.service;

import javax.jms.JMSException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import uk.nhs.adaptors.mockmhsservice.message.InboundMessage;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

import java.io.IOException;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final ObjectMapper objectMapper;
    private final InboundProducer inboundProducer;

    @Value("classpath:COPC_IN000001UK01.xml")
    private Resource xmlStubPayload;

    public ResponseEntity<String> handleRequest(String interactionId, String json) throws JMSException, IOException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
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
            inboundProducer.sendToMhsInboundQueue(readString(xmlStubPayload.getFile().toPath(), UTF_8));
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
