package uk.nhs.adaptors.gp2gp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GP2GPService {

    private final GpConnectClient gpConnectClient;
    private final FhirToHL7TranslationService fhirToHL7TranslationService;
    private final JmsTemplate jmsTemplate;

    @Value("${gp2gp.amqp.outboundQueueName}")
    protected String mhsOutboundQueueName;

    public void handleRequest(String xml) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        var getStructuredRecordRequestBody = GpConnectRequestBuilder.buildGetStructuredRecordRequestBody(xml);
        var getStructuredRecordResponseBody = gpConnectClient.getStructuredRecord(getStructuredRecordRequestBody);
        var hl7Response = fhirToHL7TranslationService.translate(getStructuredRecordResponseBody);
        LOGGER.debug("Processing completed resulting in HL7v3 message:\n{}", hl7Response);
        //TODO send hl7Response back to MHS
        jmsTemplate.send(mhsOutboundQueueName, session -> session.createTextMessage(hl7Response));
    }
}
