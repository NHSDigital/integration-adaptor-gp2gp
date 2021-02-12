package uk.nhs.adaptors.gp2gp.ehr;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrDocumentTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EhrDocumentMapper {
    private static final Mustache MHS_PAYLOAD_TEMPLATE = TemplateUtils.loadTemplate("ehr_document_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
        .toFormatter();
    private static final String UK_ZONE_ID = "Europe/London";

    private final TimestampService timestampService;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public EhrDocumentTemplateParameters mapToMhsPayloadTemplateParameters(GetGpcDocumentTaskDefinition taskDefinition, String messageId) {
        return EhrDocumentTemplateParameters.builder()
            .resourceCreated(DATE_TIME_FORMATTER.format(timestampService.now()
                .atZone(ZoneId.of(UK_ZONE_ID))))
            .messageId(messageId)
            .accessDocumentId(taskDefinition.getDocumentId())
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
