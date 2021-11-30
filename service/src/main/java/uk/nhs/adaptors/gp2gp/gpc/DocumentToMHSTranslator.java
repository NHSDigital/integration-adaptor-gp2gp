package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hl7.fhir.dstu3.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentToMHSTranslator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FhirParseService fhirParseService;
    private final EhrDocumentMapper ehrDocumentMapper;
    
    public String translateGpcResponseToMhsOutboundRequestData(
        DocumentTaskDefinition taskDefinition, String response) {

        var binary = fhirParseService.parseResource(response, Binary.class);

        return prepare(taskDefinition, binary.getContent(), binary.getContentType());
    }
    
    public String translateFileContentToMhsOutboundRequestData(
        DocumentTaskDefinition taskDefinition, String fileContent) {
        return prepare(taskDefinition, fileContent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN_VALUE);
    }

    //TODO: change prepare

    private String prepare(DocumentTaskDefinition taskDefinition, byte[] bytes, String textPlainValue) {
        var ehrDocumentTemplateParameters = ehrDocumentMapper
            .mapToMhsPayloadTemplateParameters(taskDefinition);
        var xmlContent = ehrDocumentMapper.mapMhsPayloadTemplateToXml(ehrDocumentTemplateParameters);

        try {
            return prepareOutboundMessage(taskDefinition, bytes, textPlainValue, xmlContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String prepareOutboundMessage(DocumentTaskDefinition taskDefinition, byte[] fileContent, String contentType,
        String xmlContent)
        throws JsonProcessingException {
        var attachments = Collections.singletonList(
            OutboundMessage.Attachment.builder()
                .contentType(contentType)
                .isBase64(Boolean.TRUE.toString())
                .description(taskDefinition.getDocumentId())
                .payload(Base64.getEncoder().encodeToString(fileContent))
                .build());
        var outboundMessage = OutboundMessage.builder()
            .payload(xmlContent)
            .attachments(attachments)
            .build();

        return OBJECT_MAPPER.writeValueAsString(outboundMessage);
    }
}
