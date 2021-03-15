package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AgentMapperTemplateParametersOuter {
    private String agentId;
    private String organisationInfo;
}
