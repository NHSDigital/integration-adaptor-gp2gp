package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AgentDirectoryParameter {
    private String patientManagingOrganization;
    private List<String> agentPersons;
}
