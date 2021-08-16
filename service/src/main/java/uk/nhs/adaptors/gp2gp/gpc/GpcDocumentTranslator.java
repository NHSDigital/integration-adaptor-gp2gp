package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hl7.fhir.dstu3.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.Collections;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GpcDocumentTranslator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FhirParseService fhirParseService;
    private final EhrDocumentMapper ehrDocumentMapper;

    public String translateToMhsOutboundRequestData(
        DocumentTaskDefinition taskDefinition, String response) {

        var ehrDocumentTemplateParameters = ehrDocumentMapper
            .mapToMhsPayloadTemplateParameters(taskDefinition);
        var xmlContent = ehrDocumentMapper.mapMhsPayloadTemplateToXml(ehrDocumentTemplateParameters);

        try {
            return prepareOutboundMessage(taskDefinition, response, xmlContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String prepareOutboundMessage(DocumentTaskDefinition taskDefinition, String response, String xmlContent)
            throws JsonProcessingException {

        var isResponseBinary = true;
        var attachmentBuilder = OutboundMessage.Attachment.builder()
            .isBase64(Boolean.TRUE.toString())
            .description(taskDefinition.getDocumentId());
        try {
            var binary = fhirParseService.parseResource(response, Binary.class);
            attachmentBuilder
                .contentType(binary.getContentType())
                .payload(binary.getContentAsBase64());
        } catch (FhirValidationException exception) {
            isResponseBinary = false;
        }

        if (!isResponseBinary) {
            attachmentBuilder
                .contentType("application/xml")
                .payload(response);
        }

        var attachments = Collections.singletonList(attachmentBuilder.build());
        var outboundMessage = OutboundMessage.builder()
            .payload(xmlContent)
            .attachments(attachments)
            .build();

        return OBJECT_MAPPER.writeValueAsString(outboundMessage);
    }
}
