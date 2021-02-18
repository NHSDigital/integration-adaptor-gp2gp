package uk.nhs.adaptors.mockmhsservice.controller;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.mockmhsservice.service.MockMhsService;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MhsMockController {
    private final MockMhsService mockMhsService;
    private final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("InternalServerError.html");
    private final String internalServerErrorResponse = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    private final HttpHeaders responseHeaders = new HttpHeaders();

    @PostMapping(value = "/mock-mhs-endpoint",
        consumes = APPLICATION_JSON_VALUE
    )
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required=false) String mockMhsMessage) {

        try {
            String interactionId = Optional.ofNullable(headers.get("interaction-id")).orElse(StringUtils.EMPTY);
            String correlationId = Optional.ofNullable(headers.get("correlation-id")).orElse(StringUtils.EMPTY);
            String waitForResponse = Optional.ofNullable(headers.get("wait-for-response")).orElse(StringUtils.EMPTY);
            String contentType = Optional.ofNullable(headers.get("content-type")).orElse(StringUtils.EMPTY);
            String odsCode = Optional.ofNullable(headers.get("ods-code")).orElse(StringUtils.EMPTY);
            return mockMhsService.handleRequest(interactionId, correlationId, waitForResponse, mockMhsMessage, contentType, odsCode);
        } catch (Exception e) {
            LOGGER.error("Error could not process mock request", e);
            responseHeaders.setContentType(MediaType.TEXT_HTML);
            return new ResponseEntity<>(internalServerErrorResponse, responseHeaders, INTERNAL_SERVER_ERROR);
        }
    }
}
