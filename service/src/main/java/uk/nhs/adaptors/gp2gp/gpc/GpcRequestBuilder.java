package uk.nhs.adaptors.gp2gp.gpc;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import javax.net.ssl.SSLException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.apache.http.protocol.HTTP.TARGET_HOST;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.apache.http.protocol.HTTP.CONTENT_LEN;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ca.uhn.fhir.parser.IParser;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcRequestBuilder {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";
    private final static String SSP_FROM = "Ssp-From";
    private final static String SSP_TO = "Ssp-To";
    private final static String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private final static String SSP_TRACE_ID = "Ssp-TraceID";
    private final static String AUTHORIZATION = "Authorization";

    private final IParser fhirParser;
    private final GpcTokenBuilder gpcTokenBuilder;
    private final GpcConfiguration gpcConfiguration;

    public static Parameters buildGetStructuredRecordRequestBody(String ehrXml) throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {
        var ehrDocument = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(ehrXml)));

        var xPath = XPathFactory.newInstance().newXPath();
        //TODO: more precise xpath
        var expression = "/RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/recordTarget/patient/id";
        var nodeList = (NodeList) xPath.compile(expression).evaluate(ehrDocument, XPathConstants.NODESET);

        //TODO: handle nulls
        String nhsNumber = nodeList.item(0).getAttributes().getNamedItem("extension").getNodeValue();

        return new Parameters()
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("patientNHSNumber")
                .setValue(new Identifier().setSystem(NHS_NUMBER_SYSTEM).setValue(nhsNumber)))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeAllergies")
                .addPart(new Parameters.ParametersParameterComponent()
                    .setName("includeResolvedAllergies")
                    .setValue(new BooleanType(true))))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeMedication")
                .addPart(new Parameters.ParametersParameterComponent()
                    .setName("includePrescriptionIssues")
                    .setValue(new BooleanType(true)))
                .addPart(new Parameters.ParametersParameterComponent()
                    .setName("medicationSearchFromDate")
                    .setValue(new DateType("1980-06-05"))));
    }

    // TODO: rename method
    public WebClient.RequestHeadersSpec<?> buildGetStructuredRecordRequest(Parameters requestBodyParameters) throws SSLException {

        SslContext sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

        // create webclient
        WebClient client = WebClient
            .builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(gpcConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", gpcConfiguration.getUrl()))
            .build();

        // provide url
        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(gpcConfiguration.getEndpoint());

        //body
        var requestBody = fhirParser.encodeResourceToString(requestBodyParameters);
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter
            = BodyInserters.fromValue(requestBody);

        return uri
            .body(bodyInserter)
            .accept(MediaType.valueOf(FHIR_CONTENT_TYPE))
            .header(SSP_FROM, "200000000359")
            .header(SSP_TO, "918999198738")
            .header(SSP_INTERACTION_ID, "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1")
            .header(SSP_TRACE_ID, "e4f153f8-72f1-457c-ab16-abc71b84354f")
            .header(AUTHORIZATION, "Bearer " + gpcTokenBuilder.buildToken())
            .header(TARGET_HOST, "orange.testlab.nhs.uk")
            .header(CONTENT_LEN, "524")
            .header(CONTENT_TYPE, FHIR_CONTENT_TYPE);
    }
}

