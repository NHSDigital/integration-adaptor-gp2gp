package uk.nhs.adaptors.gp2gp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.services.FhirToHL7TranslationService;
import uk.nhs.adaptors.gp2gp.services.GpConnectClient;
import uk.nhs.adaptors.gp2gp.services.GpConnectRequestBuilder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GP2GPController {

    private static final String XML_CONTENT_TYPE = "application/xml;charset=UTF-8";

    private final GpConnectClient gpConnectClient;
    private final FhirToHL7TranslationService fhirToHL7TranslationService;

    @PostMapping(path = "/structuredRecord", consumes = XML_CONTENT_TYPE, produces = XML_CONTENT_TYPE)
    public ResponseEntity<String> getStructuredRecord(@RequestBody() String xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        var getStructuredRecordRequestBody = GpConnectRequestBuilder.buildGetStructuredRecordRequestBody(xml);
        var getStructuredRecordResponseBody = gpConnectClient.getStructuredRecord(getStructuredRecordRequestBody);
        var hl7Response = fhirToHL7TranslationService.translate(getStructuredRecordResponseBody);

        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_XML)
            .body(hl7Response);
    }
}
