package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PractitionerAgentPersonMapperParameters {
    private String agentId;
    private String practitionerRole;
    private String practitionerPrefix;
    private String practitionerGivenName;
    private String practitionerFamilyName;
    private String organization;
    private boolean practitioner;
    private String practitionerUnstructuredName;
}
