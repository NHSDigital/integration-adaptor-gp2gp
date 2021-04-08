package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ParticipantTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ParticipantMapper {
    private static final Mustache PARTICIPANT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_participant_template.mustache");

    public String mapToParticipant(String reference, ParticipantType type) {
        var participantParameters = ParticipantTemplateParameters.builder()
            .typeCode(type.getCode())
            .participantId(reference)
            .build();

        return TemplateUtils.fillTemplate(PARTICIPANT_TEMPLATE, participantParameters);
    }
}
