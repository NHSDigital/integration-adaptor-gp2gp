package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsConfiguration;
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
    private static final String EHR_EXTRACT_INTERACTION_ID = "RCMR_IN030000UK06";
    private static final String EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS = "RCMR_IN030000UK07";

    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final RedactionsConfiguration redactionsConfiguration;

    public String map(GetGpcStructuredTaskDefinition getGpcDocumentTaskDefinition, String ehrExtractContent) {
        final OutputMessageWrapperTemplateParameters outputMessageWrapperTemplateParameters = OutputMessageWrapperTemplateParameters
            .builder()
            .interactionId(getInteractionId())
            .eventId(randomIdGeneratorService.createNewId())
            .creationTime(DateFormatUtil.toHl7Format(timestampService.now()))
            .fromAsid(getGpcDocumentTaskDefinition.getFromAsid())
            .toAsid(getGpcDocumentTaskDefinition.getToAsid())
            .ehrExtractContent(ehrExtractContent)
            .build();

        return TemplateUtils.fillTemplate(TEMPLATE, outputMessageWrapperTemplateParameters);
    }

    private String getInteractionId() {
        return redactionsConfiguration.isRedactionsEnabled() ? EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS : EHR_EXTRACT_INTERACTION_ID;
    }
}