package uk.nhs.adaptors.gp2gp.gpc;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu3.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrDocumentTemplateParameters;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessageWithAttachments;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GpcDocumentTranslator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FhirParseService fhirParseService;
    private final EhrDocumentMapper ehrDocumentMapper;

    public String translateToMhsOutboundRequestPayload(GetGpcDocumentTaskDefinition taskDefinition, String response, String messageId) {
        Binary binary = fhirParseService.parseResource(response, Binary.class);

        EhrDocumentTemplateParameters ehrDocumentTemplateParameters =
            ehrDocumentMapper.mapToMhsPayloadTemplateParameters(taskDefinition, messageId);
        String xmlContent = ehrDocumentMapper.mapMhsPayloadTemplateToXml(ehrDocumentTemplateParameters);

        try {
            return prepareOutboundMessage(taskDefinition, binary, xmlContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String prepareOutboundMessage(GetGpcDocumentTaskDefinition taskDefinition, Binary binary, String xmlContent)
            throws JsonProcessingException {
        List<OutboundMessageWithAttachments.Attachment> attachments = Collections.singletonList(
            new OutboundMessageWithAttachments.Attachment(binary.getContentType(),
                Boolean.TRUE.toString(),
                taskDefinition.getDocumentId(),
                binary.getContentAsBase64()));
        OutboundMessageWithAttachments outboundMessage = new OutboundMessageWithAttachments(xmlContent, attachments);

        return OBJECT_MAPPER.writeValueAsString(outboundMessage);
    }
}
