package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.OutputMessageWrapperTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OutputMessageWrapperMapper {
    private static final Mustache TEMPLATE = TemplateUtils.loadTemplate("output_message_wrapper_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
        .toFormatter();
    private static final String UK_ZONE_ID = "Europe/London";

    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;

    public String map(GetGpcStructuredTaskDefinition getGpcDocumentTaskDefinition, String ehrExtractContent) {
        OutputMessageWrapperTemplateParameters outputMessageWrapperTemplateParameters = OutputMessageWrapperTemplateParameters.builder()
            .eventId(randomIdGeneratorService.createNewId())
            .creationTime(DATE_TIME_FORMATTER.format(timestampService.now().atZone(ZoneId.of(UK_ZONE_ID))))
            .fromAsid(getGpcDocumentTaskDefinition.getFromAsid())
            .toAsid(getGpcDocumentTaskDefinition.getToAsid())
            .ehrExtractContent(ehrExtractContent)
            .build();

        return TemplateUtils.fillTemplate(TEMPLATE, outputMessageWrapperTemplateParameters);
    }
}
