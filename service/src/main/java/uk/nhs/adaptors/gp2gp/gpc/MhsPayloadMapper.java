package uk.nhs.adaptors.gp2gp.gpc;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.mhs.model.MhsPayloadTemplateParameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class MhsPayloadMapper {
    private static final Mustache MHS_PAYLOAD_TEMPLATE = TemplateUtils.loadTemplate("mhs_payload_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
        .toFormatter();

    private final TimestampService timestampService;

    public MhsPayloadTemplateParameters mapToMhsPayloadTemplateParameters(GetGpcDocumentTaskDefinition taskDefinition, String messageId) {
        return MhsPayloadTemplateParameters.builder()
            .resourceCreated(DATE_TIME_FORMATTER.format(timestampService.now()
                .atOffset(ZoneOffset.UTC)))
            .messageId(messageId)
            .accessDocumentId(taskDefinition.getDocumentId())
            .fromAsid(taskDefinition.getFromAsid())
            .toAsid(taskDefinition.getToAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .build();
    }

    public String mapMhsPayloadTemplateToXml(MhsPayloadTemplateParameters mhsPayloadTemplateParameters) {
        return TemplateUtils.fillTemplate(MHS_PAYLOAD_TEMPLATE, mhsPayloadTemplateParameters);
    }
}
