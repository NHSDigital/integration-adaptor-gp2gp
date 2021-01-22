package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EhrFhirExtractParams {
    private String patientId;
    private String authorId;
    private String destinationId;
    private String requestId;
    private String ehrFolderId;
    private String availabilityTime;
    private String effectiveTimeLow;
    private String effectiveTimeHigh;
    private String authorTime;
    private String ehrExtractId;
    private String limitationId;
    private String limitationCode;
    private String limitationCodeSystem;
    private String limitationDisplayName;
}
