package uk.nhs.adaptors.mockmhsservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.JmsException;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import uk.nhs.adaptors.mockmhsservice.common.InboundMessage;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final ObjectMapper objectMapper;
    private final InboundProducer inboundProducer;

    private final String mockSuccessMessage = "Message acknowledged.";
    private final String mockInteractionIdErrorMessage = "Error, cannot handle request header Interaction-Id.";
    private final String mockRequestBodyErrorMessage = "Error, content of request body does not match expected JSON";
    private final String mockInboundReplyErrorMessage = "Error, could not produce inbound reply.";
    private final String mockValidInteractionId = "RCMR_IN030000UK06";

    private final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("COPC_IN000001UK01.xml");
    private final String stubEbXmlText = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

    public ResponseEntity<String> handleRequest(String interactionId, String mockMhsMessage) throws IOException {

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        ObjectNode rootNode = objectMapper.createObjectNode();

        String responseJsonString;
        try {
            verifyJson(mockMhsMessage);
        } catch (JsonProcessingException e) {
            rootNode.put("message", mockRequestBodyErrorMessage);
            responseJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            return new ResponseEntity<>(responseJsonString, INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals(mockValidInteractionId)) {
            try {
                inboundProducer.sendToMhsInboundQueue(stubEbXmlText);
                rootNode.put("message", mockSuccessMessage);
                responseJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                return new ResponseEntity<>(responseJsonString, ACCEPTED);
            } catch (JmsException e) {
                rootNode.put("message", mockInboundReplyErrorMessage);
                responseJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                return new ResponseEntity<>(responseJsonString, INTERNAL_SERVER_ERROR);
            }
        }

        rootNode.put("message", mockInteractionIdErrorMessage);
        responseJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        return new ResponseEntity<>(responseJsonString, INTERNAL_SERVER_ERROR);
    }

    private void verifyJson(String json) throws JsonProcessingException {
        objectMapper.readValue(json, InboundMessage.class);
    }
}
