package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hl7.fhir.dstu3.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrDocumentTemplateParameters;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GpcDocumentTranslator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FhirParseService fhirParseService;
    private final EhrDocumentMapper ehrDocumentMapper;

    public String translateToMhsOutboundRequestData(GetGpcDocumentTaskDefinition taskDefinition, String response, String messageId) {
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
        List<OutboundMessage.Attachment> attachments = Collections.singletonList(
            new OutboundMessage.Attachment(binary.getContentType(),
                Boolean.TRUE.toString(),
                taskDefinition.getDocumentId(),
                binary.getContentAsBase64()));
        var outboundMessage = OutboundMessage.builder()
            .payload(xmlContent)
            .attachments(attachments)
            .build();

        return OBJECT_MAPPER.writeValueAsString(outboundMessage);
    }
}
