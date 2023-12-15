package uk.nhs.adaptors.gp2gp.mhs;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import com.google.common.collect.ImmutableMap;

import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gp2gp.common.service.RequestBuilderService;
import uk.nhs.adaptors.gp2gp.common.service.WebClientFilterService;
import uk.nhs.adaptors.gp2gp.mhs.configuration.MhsClientConfiguration;
import uk.nhs.adaptors.gp2gp.mhs.configuration.MhsConfiguration;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsRequestBuilder {
    private static final String ODS_CODE = "ods-code";
    private static final String INTERACTION_ID = "Interaction-Id";
    private static final String MHS_OUTBOUND_EXTRACT_CORE_INTERACTION_ID = "RCMR_IN030000UK06";
    private static final String CORRELATION_ID = "Correlation-Id";
    private static final String WAIT_FOR_RESPONSE = "wait-for-response";
    private static final String FALSE = "false";
    private static final String CONTENT_TYPE = "Content-type";
    private static final String MHS_OUTBOUND_ACKNOWLEDGEMENT_INTERACTION_ID = "MCCI_IN010000UK13";
    private static final String MHS_OUTBOUND_COMMON_INTERACTION_ID = "COPC_IN000001UK01";
    private static final String MESSAGE_ID = "Message-Id";

    private final MhsConfiguration mhsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final MhsClientConfiguration mhsClientConfig;

    public RequestHeadersSpec<?> buildSendEhrExtractCoreRequest(
            String extractCoreMessage, String conversationId, String fromOdsCode, String messageId) {
        return buildRequest(extractCoreMessage, fromOdsCode, conversationId, MHS_OUTBOUND_EXTRACT_CORE_INTERACTION_ID, messageId);
    }

    public RequestHeadersSpec<?> buildSendAcknowledgementRequest(
            String requestBody, String fromOdsCode, String conversationId, String positiveAckMessageId) {
        return buildRequest(requestBody, fromOdsCode, conversationId, MHS_OUTBOUND_ACKNOWLEDGEMENT_INTERACTION_ID, positiveAckMessageId);
    }

    public RequestHeadersSpec<?> buildSendEhrExtractCommonRequest(
            String requestBody, String conversationId, String fromOdsCode, String messageId) {
        return buildRequest(requestBody, fromOdsCode, conversationId, MHS_OUTBOUND_COMMON_INTERACTION_ID, messageId);
    }

    private RequestHeadersSpec<?> buildRequest(
        String requestBody, String fromOdsCode, String conversationId, String interactionId, String messageId) {

        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        WebClient client = buildWebClient(httpClient);

        var requestMethod = HttpMethod.POST;
        var headersBuilder = ImmutableMap.<String, String>builder()
            .put("Accept", APPLICATION_JSON.toString())
            .put(ODS_CODE, fromOdsCode)
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(INTERACTION_ID, interactionId)
            .put(WAIT_FOR_RESPONSE, FALSE)
            .put(CORRELATION_ID, conversationId);
        if (messageId != null) {
            headersBuilder.put(MESSAGE_ID, messageId);
        }
        var headers = headersBuilder.build();

        LOGGER.debug("Request: {} {}", requestMethod, mhsConfiguration.getUrl());
        headers.forEach((k, v) -> LOGGER.debug("Header: {}={}", k, v));
        LOGGER.debug("Body: {}", requestBody);

        return client
            .method(requestMethod)
            .uri(mhsConfiguration.getUrl())
            .headers(requestHeaders -> headers.forEach(requestHeaders::add))
            .body(BodyInserters.fromValue(requestBody));
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(requestBuilderService.buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filters(filters -> WebClientFilterService
                .addWebClientFilters(
                    filters, WebClientFilterService.RequestType.MHS_OUTBOUND, HttpStatus.ACCEPTED, mhsClientConfig))
            .baseUrl(mhsConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", mhsConfiguration.getUrl()))
            .build();
    }
}
