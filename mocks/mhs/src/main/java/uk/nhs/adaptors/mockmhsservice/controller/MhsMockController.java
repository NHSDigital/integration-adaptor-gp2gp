package uk.nhs.adaptors.mockmhsservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import javax.jms.JMSException;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.mockmhsservice.service.MockMhsService;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MhsMockController {
    private final MockMhsService mockMhsService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/mock-mhs-endpoint",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(
            @RequestHeader(value="Interaction-Id", required=false) String interactionId,
            @RequestBody String mockMhsMessage) throws JMSException, JsonProcessingException {
        ObjectNode rootNode = objectMapper.createObjectNode();
        String jsonString;

        try {
            return mockMhsService.handleRequest(interactionId, mockMhsMessage);
        } catch (Exception e) {
            rootNode.put("message", e.getMessage());
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            return new ResponseEntity<>(jsonString, INTERNAL_SERVER_ERROR);
        }
    }
}
