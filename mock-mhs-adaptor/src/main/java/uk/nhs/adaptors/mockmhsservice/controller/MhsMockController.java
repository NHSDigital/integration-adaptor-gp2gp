package uk.nhs.adaptors.mockmhsservice.controller;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.adaptors.mockmhsservice.service.MDCService;
import uk.nhs.adaptors.mockmhsservice.service.MockMhsService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MhsMockController {
    private final MockMhsService mockMhsService;
    private final MDCService mdcService;
    private final HttpHeaders responseHeaders = new HttpHeaders();

    private static final List<String> REQUEST_JOURNAL = new ArrayList<>();

    @PostMapping(value = "/mock-mhs-endpoint",
        consumes = APPLICATION_JSON_VALUE
    )
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required=false) String mockMhsMessage) {

        if (isRequestJournalEnabled()) {
            LOGGER.info("Logging request in journal");
            REQUEST_JOURNAL.add(mockMhsMessage);
        }

        try {
            String correlationId = Optional.ofNullable(headers.get("correlation-id")).orElse(StringUtils.EMPTY);
            mdcService.applyConversationId(correlationId);
            String interactionId = Optional.ofNullable(headers.get("interaction-id")).orElse(StringUtils.EMPTY);
            String waitForResponse = Optional.ofNullable(headers.get("wait-for-response")).orElse(StringUtils.EMPTY);
            String odsCode = Optional.ofNullable(headers.get("ods-code")).orElse(StringUtils.EMPTY);
            return mockMhsService.handleRequest(interactionId, correlationId, waitForResponse, mockMhsMessage, odsCode);
        } catch (Exception e) {
            LOGGER.error("Error could not process mock request", e);
            responseHeaders.setContentType(MediaType.TEXT_HTML);
            return new ResponseEntity<>(getInternalServerErrorResponse(), responseHeaders, INTERNAL_SERVER_ERROR);
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }

    @SneakyThrows(IOException.class)
    private String getInternalServerErrorResponse(){
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("InternalServerError.html")) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    @DeleteMapping(value = "/__admin/requests")
    public ResponseEntity<String> deleteRequestJournal() {
        REQUEST_JOURNAL.clear();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/__admin/requests")
    public ResponseEntity<List> getRequestJournal() {
        return new ResponseEntity<>(REQUEST_JOURNAL, HttpStatus.OK);
    }

    private boolean isRequestJournalEnabled() {
        var requestJournalEnabled = System.getenv("MHS_MOCK_REQUEST_JOURNAL_ENABLED");
        return Boolean.TRUE.toString().equalsIgnoreCase(requestJournalEnabled);
    }
}
