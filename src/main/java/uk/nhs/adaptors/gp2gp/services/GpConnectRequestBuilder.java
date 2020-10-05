package uk.nhs.adaptors.gp2gp.services;

import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.configurations.GpConnectConfiguration;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpConnectRequestBuilder {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";

    private final IParser fhirParser;
    private final TokenBuilder tokenBuilder;
    private final GpConnectConfiguration gpConnectConfiguration;

    public HttpPost buildGStructuredRecordRequest(Parameters parameters) {
        HttpPost httpPost = new HttpPost(gpConnectConfiguration.getUrl() + gpConnectConfiguration.getEndpoint());
        httpPost.addHeader("Ssp-From", gpConnectConfiguration.getSspFrom());
        httpPost.addHeader("Ssp-To", gpConnectConfiguration.getSspTo());
        httpPost.addHeader("Ssp-InteractionID", gpConnectConfiguration.getSspInteractionID());
        httpPost.addHeader("Ssp-TraceID", UUID.randomUUID().toString());
        httpPost.addHeader("Content-Type", FHIR_CONTENT_TYPE);
        httpPost.addHeader("Accept", FHIR_CONTENT_TYPE);
        httpPost.addHeader("Authorization", "Bearer " + tokenBuilder.buildToken());
        var requestBody = fhirParser.encodeResourceToString(parameters);
        LOGGER.debug("GPConnect request body:\n{}", requestBody);
        httpPost.setEntity(new StringEntity(requestBody, Charset.defaultCharset()));

        return httpPost;
    }

    public static Parameters buildGetStructuredRecordRequestBody(String ehrXml) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
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
}
