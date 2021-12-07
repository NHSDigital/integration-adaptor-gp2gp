package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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

    private final EhrDocumentMapper ehrDocumentMapper;

    public String translateToMhsOutboundRequestData(
        DocumentTaskDefinition taskDefinition, byte[] base64Content, String contentType) {

        var ehrDocumentTemplateParameters = ehrDocumentMapper
            .mapToMhsPayloadTemplateParameters(taskDefinition);
        var xmlContent = ehrDocumentMapper.mapMhsPayloadTemplateToXml(ehrDocumentTemplateParameters);

        try {
            return prepareOutboundMessage(taskDefinition, base64Content, contentType, xmlContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String translateFileContentToMhsOutboundRequestData(DocumentTaskDefinition taskDefinition, String fileContent) {
        return createOutboundMessage(
            taskDefinition, fileContent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN_VALUE
        );
    }

    private String createOutboundMessage(DocumentTaskDefinition taskDefinition, byte[] bytes, String textPlainValue) {
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
