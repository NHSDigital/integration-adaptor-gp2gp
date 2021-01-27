package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
