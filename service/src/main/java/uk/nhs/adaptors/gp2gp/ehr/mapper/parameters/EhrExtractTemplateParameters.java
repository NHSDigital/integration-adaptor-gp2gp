package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EhrExtractTemplateParameters {
    private String patientId;
    private String toOdsCode;
    private String fromOdsCode;
    private String requestId;
    private String ehrFolderId;
    private String availabilityTime;
    private String effectiveTimeLow;
    private String effectiveTimeHigh;
    private String ehrExtractId;
    private String agentDirectory;
    private List<String> components;
}
