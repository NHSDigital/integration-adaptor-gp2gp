package uk.nhs.adaptors.mockmhsservice.controller;

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

    @PostMapping(value = "/mock-mhs-endpoint",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(@RequestHeader(value="Interaction-Id") String interactionId, @RequestBody String mockMhsMessage) throws JMSException {
        String mockErrorMessage = "{\"message\": \"Error, something went wrong.\"}";

        try {
            return mockMhsService.handleRequest(interactionId, mockMhsMessage);
        } catch (Exception e) {
            return new ResponseEntity<>(mockErrorMessage, INTERNAL_SERVER_ERROR);
        }
    }
}
