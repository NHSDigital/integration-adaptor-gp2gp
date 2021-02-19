package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.OutputMessageWrapperTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OutputMessageWrapperMapper {
    private static final Mustache TEMPLATE = TemplateUtils.loadTemplate("output_message_wrapper_template.mustache");
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;

    public String map(GetGpcStructuredTaskDefinition getGpcDocumentTaskDefinition, String ehrExtractContent) {
        OutputMessageWrapperTemplateParameters outputMessageWrapperTemplateParameters = OutputMessageWrapperTemplateParameters.builder()
            .eventId(randomIdGeneratorService.createNewId())
            .creationTime(DateFormatUtil.formatDate(timestampService.now()))
            .fromAsid(getGpcDocumentTaskDefinition.getFromAsid())
            .toAsid(getGpcDocumentTaskDefinition.getToAsid())
            .ehrExtractContent(ehrExtractContent)
            .build();

        return TemplateUtils.fillTemplate(TEMPLATE, outputMessageWrapperTemplateParameters);
    }
}
