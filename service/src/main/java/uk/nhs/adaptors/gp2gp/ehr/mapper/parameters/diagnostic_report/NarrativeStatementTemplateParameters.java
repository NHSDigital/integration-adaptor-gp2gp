package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NarrativeStatementTemplateParameters {
    private String narrativeStatementId;
    private String commentType;
    private String availabilityTime;
    private String comment;
    private String participant;
}
