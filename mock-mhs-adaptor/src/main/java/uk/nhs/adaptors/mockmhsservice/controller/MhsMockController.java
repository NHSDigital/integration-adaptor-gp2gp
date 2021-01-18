package uk.nhs.adaptors.mockmhsservice.controller;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

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
            @RequestHeader(value="Interaction-Id", defaultValue="") String interactionId,
            @RequestHeader(value="wait-for-response", defaultValue="false") String waitForResponse,
            @RequestHeader(value="from-asid", required=false) String fromAsid,
            @RequestHeader(value="Message-Id", required=false) String messageId,
            @RequestHeader(value="Correlation-Id", required=false) String correlationId,
            @RequestHeader(value="ods-code", required=false) String odsCode,
            @RequestBody String mockMhsMessage) throws JsonProcessingException {

        ObjectNode rootNode = objectMapper.createObjectNode();
        String jsonString;

        try {
            return mockMhsService.handleRequest(interactionId, mockMhsMessage);
        } catch (IOException e) {
            rootNode.put("message", e.getMessage());
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            return new ResponseEntity<>(jsonString, INTERNAL_SERVER_ERROR);
        }
    }
}
