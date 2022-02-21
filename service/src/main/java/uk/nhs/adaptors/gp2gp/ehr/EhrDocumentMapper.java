package uk.nhs.adaptors.gp2gp.ehr;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrDocumentTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.DocumentReferenceUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EhrDocumentMapper {
    private static final Mustache MHS_PAYLOAD_TEMPLATE = TemplateUtils.loadTemplate("ehr_document_template.mustache");

    private final TimestampService timestampService;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public EhrDocumentTemplateParameters mapToMhsPayloadTemplateParameters(
            DocumentTaskDefinition taskDefinition, String contentType) {

        return EhrDocumentTemplateParameters.builder()
            .resourceCreated(DateFormatUtil.toHl7Format(timestampService.now()))
            .messageId(taskDefinition.getMessageId())
            .filename(DocumentReferenceUtils.buildPresentAttachmentFileName(taskDefinition.getDocumentId(), contentType))
            .fromAsid(taskDefinition.getFromAsid())
            .toAsid(taskDefinition.getToAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .pertinentPayloadId(randomIdGeneratorService.createNewId())
            .build();
    }

    public String mapMhsPayloadTemplateToXml(EhrDocumentTemplateParameters ehrDocumentTemplateParameters) {
        return TemplateUtils.fillTemplate(MHS_PAYLOAD_TEMPLATE, ehrDocumentTemplateParameters);
    }
}
