package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NarrativeStatementTemplateParameters {
    private String narrativeStatementId;
    private String commentType;
    private String commentDate;
    private String comment;
    private String availabilityTimeElement;
    private String confidentialityCode;
    private String participant;
}
