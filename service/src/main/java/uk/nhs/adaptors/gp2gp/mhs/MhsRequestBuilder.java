package uk.nhs.adaptors.gp2gp.mhs;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gp2gp.common.service.RequestBuilderService;
import uk.nhs.adaptors.gp2gp.common.service.WebClientFilterService;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsRequestBuilder {
    private static final String INTERACTION_ID = "Interaction-Id";
    private static final String MHS_OUTBOUND_INTERACTION_ID = "RCMR_IN030000UK06";

    private final MhsConfiguration mhsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    public RequestHeadersSpec<?> buildSendEhrExtractCoreRequest(String extractCoreMessage) {
        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        WebClient client = buildWebClient(httpClient);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(mhsConfiguration.getUrl());

        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromValue(extractCoreMessage);

        return uri
            .accept(MediaType.APPLICATION_JSON)
            .header(INTERACTION_ID, MHS_OUTBOUND_INTERACTION_ID)
            .body(bodyInserter);
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(requestBuilderService.buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(webClientFilterService.errorHandlingFilter("Mhs Outbound", HttpStatus.ACCEPTED))
            .baseUrl(mhsConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", mhsConfiguration.getUrl()))
            .build();
    }
}
