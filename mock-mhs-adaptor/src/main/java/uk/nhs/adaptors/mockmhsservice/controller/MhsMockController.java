package uk.nhs.adaptors.mockmhsservice.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MhsMockController {
    private final MockMhsService mockMhsService;

    @PostMapping(value = "/mock-mhs-endpoint",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(
            @RequestHeader Map<String, String> headers,
            @RequestBody String mockMhsMessage) throws JsonProcessingException {

        try {
            String interactionId = Optional.ofNullable(headers.get("interaction-id")).orElse("");
            return mockMhsService.handleRequest(interactionId, mockMhsMessage);
        } catch (IOException e) {
            LOGGER.error("Error could not process mock request", e);
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
        }
    }
}
