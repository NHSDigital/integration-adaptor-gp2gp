package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AgentMapperTemplateParameters {
    private String agentId;
    private String agentExtensionId;
    private String agentName;
    private String telecomValue;
    private boolean addressPresent;
    private String addressUse;
    private List<String> addressLine;
    private String postalCode;
}
