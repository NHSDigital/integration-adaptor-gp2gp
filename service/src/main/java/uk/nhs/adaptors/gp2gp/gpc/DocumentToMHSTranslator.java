package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.Collections;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentToMHSTranslator {

    private final ObjectMapper objectMapper;
    private final EhrDocumentMapper ehrDocumentMapper;

    public String translateGpcResponseToMhsOutboundRequestData(
        DocumentTaskDefinition taskDefinition, String base64Content, String contentType
    ) {
        return createOutboundMessage(taskDefinition, base64Content, contentType);
    }

    public String translateFileContentToMhsOutboundRequestData(DocumentTaskDefinition taskDefinition, String base64Content) {
        return createOutboundMessage(taskDefinition, base64Content, MediaType.TEXT_PLAIN_VALUE);
    }

    private String createOutboundMessage(DocumentTaskDefinition taskDefinition, String base64Content, String contentType) {
        try {
            return prepareOutboundMessage(
                taskDefinition,
                base64Content,
                MimeTypes.OCTET_STREAM,
                ehrDocumentMapper.generateMhsPayload(
                    taskDefinition,
                    taskDefinition.getMessageId(),
                    taskDefinition.getDocumentId(),
                    contentType
                )
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String prepareOutboundMessage(
        DocumentTaskDefinition taskDefinition, String base64Content, String contentType, String xmlContent)
        throws JsonProcessingException {

        var attachments = Collections.singletonList(
            OutboundMessage.Attachment.builder()
                .contentType(contentType)
                .isBase64(Boolean.TRUE)
                .description(taskDefinition.getDocumentId())
                .payload(base64Content)
                .build());
        var outboundMessage = OutboundMessage.builder()
            .payload(xmlContent)
            .attachments(attachments)
            .build();

        return objectMapper.writeValueAsString(outboundMessage);
    }
}
