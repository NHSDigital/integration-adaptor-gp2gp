package uk.nhs.adaptors.gp2gp.gpc;

import ca.uhn.fhir.parser.IParser;
import com.heroku.sdk.EnvKeyStore;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.lang.String.valueOf;
import static org.apache.http.protocol.HTTP.CONTENT_LEN;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.apache.http.protocol.HTTP.TARGET_HOST;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcRequestBuilder {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";
    private static final String SSP_FROM = "Ssp-From";
    private static final String SSP_TO = "Ssp-To";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String SSP_TRACE_ID = "Ssp-TraceID";
    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_BEARER = "Bearer ";
    private static final int BYTE_COUNT = 16 * 1024 * 1024;
    private static final int NUMBER_OF_RECENT_CONSULTANTS = 3;
    private static final String GPC_STRUCTURED_INTERACTION_ID = "urn:nhs:names:services:gpconnect:fhir:operation:gpc"
        + ".getstructuredrecord-1";

    private final IParser fhirParser;
    private final GpcTokenBuilder gpcTokenBuilder;
    private final GpcConfiguration gpcConfiguration;
    private final GpcWebClientFilter gpcWebClientFilter;

    public Parameters buildGetStructuredRecordRequestBody(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return new Parameters()
            .addParameter(buildParamterComponent("patientNHSNumber")
                .setValue(new Identifier().setSystem(NHS_NUMBER_SYSTEM).setValue(structuredTaskDefinition.getNhsNumber())))
            .addParameter(buildParamterComponent("includeAllergies")
                .addPart(buildParamterComponent("includeResolvedAllergies")
                    .setValue(new BooleanType(true))))
            .addParameter(buildParamterComponent("includeMedication"))
            .addParameter(buildParamterComponent("includeConsultations")
                .addPart(buildParamterComponent("includeNumberOfMostRecent")
                    .setValue(new IntegerType(NUMBER_OF_RECENT_CONSULTANTS))))
            .addParameter(buildParamterComponent("includeProblems"))
            .addParameter(buildParamterComponent("includeImmunisations"))
            .addParameter(buildParamterComponent("includeUncategorisedData"))
            .addParameter(buildParamterComponent("includeInvestigations"))
            .addParameter(buildParamterComponent("includeReferrals"));
    }

    private ParametersParameterComponent buildParamterComponent(String parameterName) {
        return new ParametersParameterComponent()
            .setName(parameterName);
    }

    public RequestHeadersSpec<?> buildGetStructuredRecordRequest(Parameters requestBodyParameters,
        GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        SslContext sslContext = buildSSLContext();
        HttpClient httpClient = buildHttpClient(sslContext);
        WebClient client = buildWebClient(httpClient);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(gpcConfiguration.getStructuredEndpoint());

        var requestBody = fhirParser.encodeResourceToString(requestBodyParameters);
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter
            = BodyInserters.fromValue(requestBody);

        return buildRequestWithHeadersAndBody(uri, requestBody, bodyInserter, structuredTaskDefinition);
    }

    @SneakyThrows
    private SslContext buildSSLContext() {
        if (shouldBuildSslContext()) {
            return buildSSLContextWithClientCertificates();
        }
        return SslContextBuilder.forClient().build();
    }

    private HttpClient buildHttpClient(SslContext sslContext) {
        var httpClient =  HttpClient.create()
            .secure(t -> t.sslContext(sslContext));

        if (Boolean.parseBoolean(gpcConfiguration.getEnableProxy())) {

            LOGGER.info("Using HTTP Proxy {}:{} for GP Connect API", gpcConfiguration.getProxy(), gpcConfiguration.getProxyPort());
            return httpClient
                .proxy(spec -> spec.type(ProxyProvider.Proxy.HTTP)
                    .host(gpcConfiguration.getProxy())
                    .port(Integer.parseInt(gpcConfiguration.getProxyPort())));
        } else {
            return httpClient;
        }
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filters(this::addWebClientFilters)
            .baseUrl(gpcConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", gpcConfiguration.getUrl()))
            .build();
    }

    private void addWebClientFilters(List<ExchangeFilterFunction> filters) {
        filters.add(gpcWebClientFilter.errorHandlingFilter());
        filters.add(gpcWebClientFilter.logRequest());
    }

    private RequestHeadersSpec<?> buildRequestWithHeadersAndBody(RequestBodySpec uri, String requestBody,
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter, GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return uri
            .body(bodyInserter)
            .accept(MediaType.valueOf(FHIR_CONTENT_TYPE))
            .header(SSP_FROM, structuredTaskDefinition.getFromAsid())
            .header(SSP_TO, structuredTaskDefinition.getToAsid())
            .header(SSP_INTERACTION_ID, GPC_STRUCTURED_INTERACTION_ID)
            .header(SSP_TRACE_ID, structuredTaskDefinition.getConversationId())
            .header(AUTHORIZATION, AUTHORIZATION_BEARER + gpcTokenBuilder.buildToken(structuredTaskDefinition.getFromOdsCode()))
            .header(TARGET_HOST, gpcConfiguration.getHost())
            .header(CONTENT_LEN, valueOf(requestBody.length()))
            .header(CONTENT_TYPE, FHIR_CONTENT_TYPE);
    }

    private boolean shouldBuildSslContext() {
        var clientKey = gpcConfiguration.getClientKey();
        var clientCert = gpcConfiguration.getClientCert();
        var rootCert = gpcConfiguration.getRootCA();
        var subCert = gpcConfiguration.getSubCA();
        final int allSslProperties = 4;

        var missingSslProperties = new ArrayList<String>();
        if (StringUtils.isBlank(clientKey)) {
            missingSslProperties.add("GP2GP_SPINE_CLIENT_KEY");
        }
        if (StringUtils.isBlank(clientCert)) {
            missingSslProperties.add("GP2GP_SPINE_CLIENT_CERT");
        }
        if (StringUtils.isBlank(rootCert)) {
            missingSslProperties.add("GP2GP_SPINE_ROOT_CA_CERT");
        }
        if (StringUtils.isBlank(subCert)) {
            missingSslProperties.add("GP2GP_SPINE_SUB_CA_CERT");
        }

        if (missingSslProperties.size() == allSslProperties) {
            LOGGER.debug("No TLS MA properties were provided. Not configuring an SSL context.");
            return false;
        } else if (missingSslProperties.isEmpty()) {
            LOGGER.debug("All TLS MA properties were provided. Configuration an SSL context.");
            return true;
        } else {
            throw new GpConnectException("All or none of the GP2GP_SPINE_ variables must be defined. Missing variables: "
                + String.join(",", missingSslProperties));
        }
    }

    @SneakyThrows
    private SslContext buildSSLContextWithClientCertificates() {
        var caCertChain = gpcConfiguration.getFormattedSubCA() + gpcConfiguration.getFormattedRootCA();

        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings(
            gpcConfiguration.getFormattedClientKey(), gpcConfiguration.getFormattedClientCert(),
            randomPassword).keyStore();

        KeyStore ts = EnvKeyStore.createFromPEMStrings(caCertChain, randomPassword).keyStore();

        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, randomPassword.toCharArray());

        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ts);

        return SslContextBuilder
            .forClient()
            .keyManager(keyManagerFactory)
            .trustManager(trustManagerFactory)
            .build();
    }

    private ExchangeStrategies buildExchangeStrategies() {
        return ExchangeStrategies
            .builder()
            .codecs(
                configurer -> configurer.defaultCodecs()
                    .maxInMemorySize(BYTE_COUNT)).build();
    }
}

