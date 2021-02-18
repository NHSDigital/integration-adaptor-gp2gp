package uk.nhs.adaptors.gp2gp.mhs;

import static org.apache.http.client.utils.URLEncodedUtils.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    private static final String ODS_CODE = "ods-code";
    private static final String INTERACTION_ID = "Interaction-Id";
    private static final String MHS_OUTBOUND_INTERACTION_ID = "RCMR_IN030000UK06";
    private static final String CORRELATION_ID = "Correlation-Id";
    private static final String WAIT_FOR_RESPONSE = "wait-for-response";
    private static final String FALSE = "false";

    private final MhsConfiguration mhsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    public RequestHeadersSpec<?> buildSendEhrExtractCoreRequest(String extractCoreMessage, String conversationId, String fromOdsCode) {
        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        WebClient client = buildWebClient(httpClient);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(mhsConfiguration.getUrl());

        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromValue(extractCoreMessage);

        return uri
            .accept(APPLICATION_JSON)
            .header(ODS_CODE, fromOdsCode)
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header(INTERACTION_ID, MHS_OUTBOUND_INTERACTION_ID)
            .header(WAIT_FOR_RESPONSE, FALSE)
            .header(CORRELATION_ID, conversationId)
            .body(bodyInserter);
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(requestBuilderService.buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(webClientFilterService.errorHandlingFilter(WebClientFilterService.RequestType.MHS_OUTBOUND, HttpStatus.ACCEPTED))
            .baseUrl(mhsConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", mhsConfiguration.getUrl()))
            .build();
    }
}
