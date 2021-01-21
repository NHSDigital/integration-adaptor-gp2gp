package uk.nhs.adaptors.gp2gp.gpc;

import static java.lang.String.valueOf;

import static org.apache.http.protocol.HTTP.CONTENT_LEN;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.apache.http.protocol.HTTP.TARGET_HOST;


import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

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
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
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
        //Symmetric Keys (referred to as Secret Keys in the JCE),
        //Asymmetric Keys (referred to as Public and Private Keys in the JCE), and
        //Trusted Certificates

        var password = gpcConfiguration.getKeystorePassword();
        var privateKey = gpcConfiguration.getEndpointKey();
        var privateCert = gpcConfiguration.getEndpointCert();
        var rootAndSubCert = gpcConfiguration.getRootSubCert();

        //private key and public cert
        KeyStore keystore = EnvKeyStore.createFromPEMStrings(privateKey, privateCert, password).keyStore();

        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, password.toCharArray());

        //ca cert trust
        KeyStore trustKeyStore = EnvKeyStore.createFromPEMStrings(rootAndSubCert, password).keyStore();

        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustKeyStore);


        return SslContextBuilder
            .forClient()
            .keyManager(keyManagerFactory)
            .trustManager(trustManagerFactory)
            .build();

        // trust manager factory should not be used in production
        /*
        * `endpoint.crt` - VPN endpoint certificate
`endpoint.key` - VPN endpoint private key
`opentest.ca-bundle` - root and sub-ca certs copied into the same file
* */
    }

//    @SneakyThrows
//    private SSLContext defaultSSLContext() {
//        KeyStore ks = EnvKeyStore.createFromPEMStrings(meshConfig.getEndpointPrivateKey(), meshConfig.getEndpointCert(), meshConfig.getMailboxPassword()).keyStore();
//        return SSLContexts.custom().loadKeyMaterial(ks, this.meshConfig.getMailboxPassword().toCharArray())
//            .loadTrustMaterial(TrustAllStrategy.INSTANCE) //TODO: TrustAllStrategy works for fake mesh - in production TrustSelfSignedStrategy should be used
//            .build();
//    }

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

    private ExchangeStrategies buildExchangeStrategies() {
        return ExchangeStrategies
            .builder()
            .codecs(
                configurer -> configurer.defaultCodecs()
                    .maxInMemorySize(BYTE_COUNT)).build();
    }
}

