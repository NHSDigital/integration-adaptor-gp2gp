package uk.nhs.adaptors.gp2gp.mhs;

import io.micrometer.core.instrument.util.IOUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsRequestBuilder {
    private static final String INTERACTION_ID = "Interaction-Id";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String MHS_OUTBOUND_INTERACTION_ID = "RCMR_IN030000UK06";
    private static final int BYTE_COUNT = 16 * 1024 * 1024;

    private final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("StubEhrExtractCoreMessage.json");
    private final String stubExtractCoreMessage = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

    private final MhsConfiguration mhsConfiguration;
    private final MhsWebClientFilter mhsWebClientFilter;

    public RequestHeadersSpec<?> buildSendEhrExtractCoreRequest(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition) {
        SslContext sslContext = buildSSLContext();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        WebClient client = buildWebClient(httpClient);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(mhsConfiguration.getUrl());

        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromValue(stubExtractCoreMessage);

        return buildRequestWithHeadersAndBody(uri, stubExtractCoreMessage, bodyInserter,
            sendEhrExtractCoreTaskDefinition, MHS_OUTBOUND_INTERACTION_ID);
    }

    @SneakyThrows
    private SslContext buildSSLContext() {
        return SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(mhsWebClientFilter.errorHandlingFilter())
            .baseUrl(mhsConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", mhsConfiguration.getUrl())) // comment this line out?
            .build();
    }

    private RequestHeadersSpec<?>  buildRequestWithHeadersAndBody(WebClient.RequestBodySpec uri, String requestBody,
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter, TaskDefinition taskDefinition, String interactionId) {

        return uri.accept(MediaType.valueOf(JSON_CONTENT_TYPE))
            .header(INTERACTION_ID, interactionId)
            .body(bodyInserter);
    }

    private ExchangeStrategies buildExchangeStrategies() {
        return ExchangeStrategies
            .builder()
            .codecs(
                configurer -> configurer.defaultCodecs()
                    .maxInMemorySize(BYTE_COUNT)).build();
    }
}
