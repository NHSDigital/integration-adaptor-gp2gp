package uk.nhs.adaptors.mockmhsservice.controller;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import uk.nhs.adaptors.mockmhsservice.service.MockMhsService;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MhsMockController {
    private final MockMhsService mockMhsService;
    private final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("InternalServerError.html");
    private final String internalServerErrorResponse = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    private final HttpHeaders responseHeaders = new HttpHeaders();

    @PostMapping(value = "/mock-mhs-endpoint")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required=false) String mockMhsMessage) {

        try {
            String interactionId = Optional.ofNullable(headers.get("interaction-id")).orElse("");
            return mockMhsService.handleRequest(interactionId, mockMhsMessage);
        } catch (Exception e) {
            LOGGER.error("Error could not process mock request", e);
            responseHeaders.setContentType(MediaType.TEXT_HTML);
            return new ResponseEntity<>(internalServerErrorResponse, responseHeaders, INTERNAL_SERVER_ERROR);
        }
    }
}
