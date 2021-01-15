package uk.nhs.adaptors.mockmhsservice.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.JmsException;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import uk.nhs.adaptors.mockmhsservice.common.InboundMessage;
import uk.nhs.adaptors.mockmhsservice.producer.InboundProducer;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MockMhsService {

    private final ObjectMapper objectMapper;
    private final InboundProducer inboundProducer;

//    @Value("classpath:COPC_IN000001UK01.xml")
//    private Resource xmlStubPayload;

    public ResponseEntity<String> handleRequest(
            String interactionId,
            String waitForResponse,
            String fromAsid,
            String messageId,
            String correlationId,
            String odsCode,
            String mockMhsMessage) throws IOException {

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        ObjectNode rootNode = objectMapper.createObjectNode();

        String responseJsonString;
        String mockSuccessMessage = "Message acknowledged.";
        String mockInteractionIdErrorMessage = "Error, cannot handle request header Interaction-Id.";
        String mockRequestBodyErrorMessage = "Error, content of request body does not match expected JSON";
        String mockInboundReplyErrorMessage = "Error, could not produce inbound reply.";

        String mockValidInteractionId = "RCMR_IN030000UK06";

        try {
            verifyJson(mockMhsMessage);
        } catch (JsonProcessingException e) {
            rootNode.put("message", mockRequestBodyErrorMessage);
            responseJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            return new ResponseEntity<>(responseJsonString, INTERNAL_SERVER_ERROR);
        }

        if (interactionId.equals(mockValidInteractionId)) {
            try {
//                System.out.println(this.xmlStubPayload.getFile().toPath());
                //ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.getClass().getClassLoader().getResource("COPC_IN000001UK01.xml").getPath());

                var inputStream = this.getClass().getClassLoader().getResourceAsStream("COPC_IN000001UK01.xml");
                var text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);


                //                inboundProducer.sendToMhsInboundQueue(readString(this.getClass().getClassLoader().getResource("COPC_IN000001UK01.xml").getPath().get, UTF_8));
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
