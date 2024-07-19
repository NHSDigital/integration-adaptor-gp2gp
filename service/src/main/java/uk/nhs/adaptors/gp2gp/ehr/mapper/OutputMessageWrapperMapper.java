package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.OutputMessageWrapperTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Component
@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
public class OutputMessageWrapperMapper {
    private static final Mustache TEMPLATE = TemplateUtils.loadTemplate("output_message_wrapper_template.mustache");

    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final RedactionsContext redactionsContext;

    public String map(GetGpcStructuredTaskDefinition getGpcDocumentTaskDefinition, String ehrExtractContent) {
        final OutputMessageWrapperTemplateParameters outputMessageWrapperTemplateParameters = OutputMessageWrapperTemplateParameters
            .builder()
            .interactionId(redactionsContext.ehrExtractInteractionId())
            .eventId(randomIdGeneratorService.createNewId())
            .creationTime(DateFormatUtil.toHl7Format(timestampService.now()))
            .fromAsid(getGpcDocumentTaskDefinition.getFromAsid())
            .toAsid(getGpcDocumentTaskDefinition.getToAsid())
            .ehrExtractContent(ehrExtractContent)
            .build();

        return TemplateUtils.fillTemplate(TEMPLATE, outputMessageWrapperTemplateParameters);
    }
}