package uk.nhs.adaptors.gp2gp.gpc;

import static java.lang.String.valueOf;

import static org.apache.http.protocol.HTTP.CONTENT_LEN;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.apache.http.protocol.HTTP.TARGET_HOST;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import com.heroku.sdk.EnvKeyStore;

import ca.uhn.fhir.parser.IParser;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

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

    private SslContext buildSSLContext() {
        if (Boolean.parseBoolean(gpcConfiguration.getEnableTLS())) {
            return buildSSLContextSpine();
        } else {
            return buildSSLContextDefault();
        }
    }

    private HttpClient buildHttpClient(SslContext sslContext) {
        var httpClient =  HttpClient.create()
            .secure(t -> t.sslContext(sslContext));

        if (Boolean.parseBoolean(gpcConfiguration.getEnableProxy())){
            return httpClient
                .proxy(spec -> spec.type(ProxyProvider.Proxy.HTTP)
                    .host(gpcConfiguration.getProxy())
                    .port(Integer.parseInt(gpcConfiguration.getProxyPort())));
//                    .nonProxyHosts("localhost|orange.testlab.nhs.uk|messagingportal.opentest.hscic.gov.uk"));
        } else {
            return httpClient;
        }
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(gpcWebClientFilter.errorHandlingFilter())
            .baseUrl(gpcConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", gpcConfiguration.getUrl()))
            .build();
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

    @SneakyThrows
    private SslContext buildSSLContextSpine() {
        var clientKey = gpcConfiguration.getClientKey();
        var clientCert = gpcConfiguration.getClientCert();
        var rootCert = gpcConfiguration.getRootCA();
        var subCert = gpcConfiguration.getSubCA();

        var invalidSslValues = new ArrayList<String>();
        if (StringUtils.isBlank(clientKey)) {
            invalidSslValues.add("client key");
        }
        if (StringUtils.isBlank(clientCert)) {
            invalidSslValues.add("client cert");
        }
        if (StringUtils.isBlank(rootCert)) {
            invalidSslValues.add("root cacert");
        }
        if (StringUtils.isBlank(subCert)) {
            invalidSslValues.add("sub cacert");
        }
        if (!invalidSslValues.isEmpty()) {
            throw new GpConnectException(String.format("Spine SSL %s %s not set",
                String.join(", ", invalidSslValues),
                invalidSslValues.size() == 1 ? "is" : "are"));
        }
        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings("-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEAwvlJ9RKW/bN16WhrZJgOIdWIoGfa9M/rbrWOAV1hGuIJyqvx\nngiJy45Fe7gFYtJeEkFOTQJ0B0PJx1VgEMhdX8PnLvi4YqG3hN5md39HKfFBYs/q\nKR9rV93J0WZq8tiryRfMqxgzdv1u36ssyU+Kme6W8lg1J0lM2Gh9/xsCX4+BKjXh\n9tRlchsBdKFWnHtO4yB7UfCf+gK9cBzfTcxQ+T6Bk4uvEzV3QIXOamYjfqd1DMC7\nlzVS2MvX7Y3ns9wNKTIpv4WxQvootGqYs54Efrcox8cO7h6l411iCbzjQCwvT6nl\nCW12QAKb32ryZ31/VH4lucBMMMyTT1j9y4JPmwIDAQABAoIBAQDCwbEWlJMuqOzH\nUf60ZQ74zQvE7vjQQkCyPbiztEsjR1bwlACuE2lRY7QUeSUoKWq+YW2Njz3HY/dS\nnf1vxjU/S7jKOrg0DcX7ewxvoTu8sbjWs0j7+t6GzoyiFuQN4FD4dkWDQpFl8pGl\n6p66GDIiwSkWuvWdYKLaKMZy5M/iEv0JP+Omny581XUV6YUigV+RGTy7N1Tbf520\nLvpF57uDFM+92L5yis5ARXyhkzSk/b+27wX8MJLuPBLl4WqfY9Oyh2RdWXVzD3cr\nPhaREmRGjeRgmyAEUMGAChvdutOlsXL5aE7vKiBZZ9r58PPMDDDmOs0wyz/cBaiW\nvmAinUQBAoGBAPyOSQLj1eGJbp7QayWwYv6kfBBVdHCLeDQYF+BACbhKpu01pq21\ny5Uz9DHzK9pk2555QmIa25yxPCgYnE9Da7KzidUumIT6VduKSPltTavA9enfup25\nWF/UT/i3WB6wOHqChZDXnCCWy2ktYCfJvCC18gZ4gr3PVVDnJ/MANRzBAoGBAMWh\n+aXEXZGIfWOBFbS9JJmgzzkyBy5zb3CFPQhbfnQZfVIonYj4RC6nCVE7AYLp2/Zn\nw0/2dX7ZMqSHz5ZfDQNu01HDZYtW22JtZ0kO5839yRYuGKQDn61jUYCRF7WDMwXx\nGylGRgb3jQZzzVuZiDGXdm27t5AKqVPgs/i9O9dbAoGAdiEfaWikG/Aqe8JEu8Y7\nJ5xMI1+1LQcvXD6AvHV3lmnklkNoCQxlgw5gBBUXx/kw+HkDYdvqKOqFlsOcKT8B\n2v47VCmNUXW+Pwf5hiFoGRQScigho7CT847dMRqg7wnCARuX2d3fuyaNUk7VEQc9\nJZe08u3fSWT3JPZaK0FHloECgYEAtUayJ6OrM79fS/LuRv8q4COJj+vcfHXzpOEB\nr3XE0pdCxSZuAWG/oI8kU7bs1vjNAwIAO4tUEIsHEbVk6oe69wgjmx7AOIPt7SVu\nlKuwYdjEJq0XevG7+B1ed7AecCasmWmjQUOtHdzZJS75EWkNbHeCcHq7j8rHsCEu\n0xw4FHsCgYEAqKTXhsEWEUOSJHUv/y7Z/Y1aBK06UEz42RyWYf8Jyi3bCWwuClbU\ncKIzSKCjAT38sHNrcFAhK6AivL9GnB9KSBWtN4W6ZsSTS0CrORK0jIyaDB/Rpk03\nVAx0e5aAavWe5gHg9oBB44ij1OJUZQ/8qrIudFNLKxNAKvt6C31fjDw=\n-----END RSA PRIVATE KEY-----",
            "-----BEGIN CERTIFICATE-----\nMIIEMDCCAxigAwIBAgIJAMEDR0Dg/TcyMA0GCSqGSIb3DQEBCwUAMFcxFDASBgNV\nBAoTC05IUyBEaWdpdGFsMREwDwYDVQQLEwhPcGVudGVzdDEsMCoGA1UEAxMjT3Bl\nbnRlc3QgRW5kcG9pbnQgSXNzdWluZyBBdXRob3JpdHkwHhcNMTkxMDE4MTQxOTIw\nWhcNMjkxMDAzMTQxOTIwWjBZMRQwEgYDVQQKDAtOSFMgRGlnaXRhbDERMA8GA1UE\nCwwIb3BlbnRlc3QxLjAsBgNVBAMMJXZwbi1jbGllbnQtMTUyNi5vcGVudGVzdC5o\nc2NpYy5nb3YudWswggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDC+Un1\nEpb9s3XpaGtkmA4h1YigZ9r0z+tutY4BXWEa4gnKq/GeCInLjkV7uAVi0l4SQU5N\nAnQHQ8nHVWAQyF1fw+cu+LhiobeE3mZ3f0cp8UFiz+opH2tX3cnRZmry2KvJF8yr\nGDN2/W7fqyzJT4qZ7pbyWDUnSUzYaH3/GwJfj4EqNeH21GVyGwF0oVace07jIHtR\n8J/6Ar1wHN9NzFD5PoGTi68TNXdAhc5qZiN+p3UMwLuXNVLYy9ftjeez3A0pMim/\nhbFC+ii0apizngR+tyjHxw7uHqXjXWIJvONALC9PqeUJbXZAApvfavJnfX9UfiW5\nwEwwzJNPWP3Lgk+bAgMBAAGjgfwwgfkwCQYDVR0TBAIwADAOBgNVHQ8BAf8EBAMC\nBaAwIAYDVR0lAQH/BBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMB0GA1UdDgQWBBT8\nYitDGwOa0mddxN+Lo4Huuk9EQTAeBglghkgBhvhCAQ0EERYPeGNhIGNlcnRpZmlj\nYXRlMHsGA1UdIwR0MHKAFCDaEuVM9WXP/3P79xE0fUxkvBrHoU+kTTBLMRQwEgYD\nVQQKEwtOSFMgRGlnaXRhbDERMA8GA1UECxMIT3BlbnRlc3QxIDAeBgNVBAMTF09w\nZW50ZXN0IFJvb3QgQXV0aG9yaXR5ggkA6u0cXbLqKoMwDQYJKoZIhvcNAQELBQAD\nggEBADL8eJiPue9JIRL5EwRJt9J+KLK7YWkxuMcByRq+O4EQwOq61gyjWOHjjxmG\nhaW6uAB559vkpe/QtZc76wtqaqnGGaavWqwXNgn30ekmYmREUZUX3W7vUKM4TuG6\nVuE0rIUfu0NelONff52HzQuv+un58IAs2XFiWU3vvnyGAuiumlyTmmZR0gPilXik\nXF4HBn5Kib4mlBdZWys8iMvbbHygekeO++TFvXWhyLHqQ0BtYoD2boBXfAbQBG37\nMQO1KIM9qMQpdJhUuBCxJ+Luo98CwNgdKqS9uok/0PLdQSswRbe1at3TVXKU7Qx6\nBQ4WsA4Xa4O6X+XpmioikLChFyQ=\n-----END CERTIFICATE-----",
            randomPassword).keyStore();

//        KeyStore ks = EnvKeyStore.createFromPEMStrings(
//            clientKey, clientCert,
//            randomPassword).keyStore();
//        KeyStore ts = EnvKeyStore.createFromPEMStrings(
//            rootCert + subCert,
//            randomPassword).keyStore();

        var testRoot = "-----BEGIN CERTIFICATE-----\nMIID0zCCArugAwIBAgIJAOrtHF2y6iqDMA0GCSqGSIb3DQEBCwUAMEsxFDASBgNV\nBAoTC05IUyBEaWdpdGFsMREwDwYDVQQLEwhPcGVudGVzdDEgMB4GA1UEAxMXT3Bl\nbnRlc3QgUm9vdCBBdXRob3JpdHkwHhcNMTkxMDE4MTMyMTM3WhcNMjkxMDE0MTMy\nMTM3WjBXMRQwEgYDVQQKEwtOSFMgRGlnaXRhbDERMA8GA1UECxMIT3BlbnRlc3Qx\nLDAqBgNVBAMTI09wZW50ZXN0IEVuZHBvaW50IElzc3VpbmcgQXV0aG9yaXR5MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu4pxlFvYpYDkRa0XBwyO2+He\ng9lrtFgxqv/7JTAKn3CIE173rtLOYDr4ugbFSQNsDYJfYPI9E4g3+mFI6FUEE7dL\nzbNtxoJsB0ns7gLWDKP4PPVY5RgIMnp4MT1awSvjDl9L2gorjUO9Kc243BQFHqr8\nPXRnf+9mogulbJSOJ0OwhGdMKeu9BRRsHPfL8ZZzpuT0yfE3KcF/Fq8tyJyK6H1x\n1t4OOPZnWZH6Sf+wdxmhBZ00kSzQ8xLHgcE9CIIjL+BTENm2DkMCcBJibxeNLEqA\nsu3ddnVRmEAAF4EiO68F7qt8AsgQO7hZPeIZy7rTUi4QfazgZeI/wgtDVt5lZQID\nAQABo4GtMIGqMB0GA1UdDgQWBBQg2hLlTPVlz/9z+/cRNH1MZLwaxzB7BgNVHSME\ndDBygBQgT0ZB8NPTyqvJG2dXVOJ6f6W0eKFPpE0wSzEUMBIGA1UEChMLTkhTIERp\nZ2l0YWwxETAPBgNVBAsTCE9wZW50ZXN0MSAwHgYDVQQDExdPcGVudGVzdCBSb290\nIEF1dGhvcml0eYIJAL42cEBDgYf+MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEL\nBQADggEBAA88rHaN440jETrRngYFfVLf/JgXI6zokvzc5/t9XkVqjG1yy1AFjPoM\n1qa0w6MhOVeqX7jKj7Y2aLBOKIKPduLIsxV43DLmmrgM+xLixxQSWv1503QvGzcF\nR3NfWpigPTaltx1ERqqJbvosqA7SNQsjz3ldI8J7I7VfkUUn0E3L7C2NPouyGh1h\nDWMB1rzU5E/oWSbqSZE3khlc9r497AWGghd6eoYCWFZiiQdcl4kDKGozja37nIUy\nUqzclg+HrNLhbVuuieINo6t7JCYV048vXjDcl93o7DOsEL0Uk3JWmuv2nclEYL1s\nlD7W2hACBjl2MP8E/iFx10iOQUwdMPE=\n-----END CERTIFICATE-----";
        var testSub = "-----BEGIN CERTIFICATE-----\nMIID0zCCArugAwIBAgIJAOrtHF2y6iqDMA0GCSqGSIb3DQEBCwUAMEsxFDASBgNV\nBAoTC05IUyBEaWdpdGFsMREwDwYDVQQLEwhPcGVudGVzdDEgMB4GA1UEAxMXT3Bl\nbnRlc3QgUm9vdCBBdXRob3JpdHkwHhcNMTkxMDE4MTMyMTM3WhcNMjkxMDE0MTMy\nMTM3WjBXMRQwEgYDVQQKEwtOSFMgRGlnaXRhbDERMA8GA1UECxMIT3BlbnRlc3Qx\nLDAqBgNVBAMTI09wZW50ZXN0IEVuZHBvaW50IElzc3VpbmcgQXV0aG9yaXR5MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu4pxlFvYpYDkRa0XBwyO2+He\ng9lrtFgxqv/7JTAKn3CIE173rtLOYDr4ugbFSQNsDYJfYPI9E4g3+mFI6FUEE7dL\nzbNtxoJsB0ns7gLWDKP4PPVY5RgIMnp4MT1awSvjDl9L2gorjUO9Kc243BQFHqr8\nPXRnf+9mogulbJSOJ0OwhGdMKeu9BRRsHPfL8ZZzpuT0yfE3KcF/Fq8tyJyK6H1x\n1t4OOPZnWZH6Sf+wdxmhBZ00kSzQ8xLHgcE9CIIjL+BTENm2DkMCcBJibxeNLEqA\nsu3ddnVRmEAAF4EiO68F7qt8AsgQO7hZPeIZy7rTUi4QfazgZeI/wgtDVt5lZQID\nAQABo4GtMIGqMB0GA1UdDgQWBBQg2hLlTPVlz/9z+/cRNH1MZLwaxzB7BgNVHSME\ndDBygBQgT0ZB8NPTyqvJG2dXVOJ6f6W0eKFPpE0wSzEUMBIGA1UEChMLTkhTIERp\nZ2l0YWwxETAPBgNVBAsTCE9wZW50ZXN0MSAwHgYDVQQDExdPcGVudGVzdCBSb290\nIEF1dGhvcml0eYIJAL42cEBDgYf+MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEL\nBQADggEBAA88rHaN440jETrRngYFfVLf/JgXI6zokvzc5/t9XkVqjG1yy1AFjPoM\n1qa0w6MhOVeqX7jKj7Y2aLBOKIKPduLIsxV43DLmmrgM+xLixxQSWv1503QvGzcF\nR3NfWpigPTaltx1ERqqJbvosqA7SNQsjz3ldI8J7I7VfkUUn0E3L7C2NPouyGh1h\nDWMB1rzU5E/oWSbqSZE3khlc9r497AWGghd6eoYCWFZiiQdcl4kDKGozja37nIUy\nUqzclg+HrNLhbVuuieINo6t7JCYV048vXjDcl93o7DOsEL0Uk3JWmuv2nclEYL1s\nlD7W2hACBjl2MP8E/iFx10iOQUwdMPE=\n-----END CERTIFICATE-----";

        KeyStore ts = EnvKeyStore.createFromPEMStrings(
            testRoot + testSub,
            randomPassword).keyStore();

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

    @SneakyThrows
    private SslContext buildSSLContextDefault() {
        return SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
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

