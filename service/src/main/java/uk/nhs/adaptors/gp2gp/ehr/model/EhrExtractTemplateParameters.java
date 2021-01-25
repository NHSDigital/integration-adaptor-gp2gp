package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EhrExtractTemplateParameters {
    private String patientId;
    private String authorOdsCode;
    private String toAsid;
    private String destinationOdsCode;
    private String requestId;
    private String ehrFolderId;
    private String availabilityTime;
    private String effectiveTimeLow;
    private String effectiveTimeHigh;
    private String ehrExtractId;
    private String limitationId;
    private String limitationCode;
    private String limitationCodeSystem;
    private String limitationDisplayName;
}
