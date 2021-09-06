package uk.nhs.adaptors.gp2gp.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.e2e.model.OutboundMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

public class MhsMockRequestsJournal {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder().build();

    private final String mhsMockBaseUrl;

    public MhsMockRequestsJournal(String mhsMockBaseUrl) {
        this.mhsMockBaseUrl = mhsMockBaseUrl;
    }

    public List<OutboundMessage> getRequestsJournal() throws IOException, InterruptedException {
        var responseBody = client
            .send(buildGetRequest(), HttpResponse.BodyHandlers.ofString())
            .body();

        List<String> requestsAsStrings = OBJECT_MAPPER.readerForListOf(String.class).readValue(responseBody);
        return requestsAsStrings.stream()
            .map(str -> {
                try {
                    return OBJECT_MAPPER.readValue(str,OutboundMessage.class);
                } catch (JsonProcessingException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .collect(Collectors.toList());
    }

    @SneakyThrows
    public void deleteRequestsJournal() {
        var statusCode = client.send(buildDeleteRequest(), HttpResponse.BodyHandlers.ofString()).statusCode();
        if (statusCode != 200) {
            throw new RuntimeException("Unexpected status code: " + statusCode);
        }
    }

    private HttpRequest.Builder buildRequest() {
        return HttpRequest.newBuilder()
            .uri(URI.create(mhsMockBaseUrl + "/__admin/requests"));
    }

    private HttpRequest buildGetRequest() {
        return buildRequest().GET().build();
    }

    private HttpRequest buildDeleteRequest() {
        return buildRequest().DELETE().build();
    }
}
