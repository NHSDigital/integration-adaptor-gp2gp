package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ObservationStatementTemplateParameters {
    private String observationStatementId;
    private String code;
    private String effectiveTime;
    private String issuedDate;
    private String value;
    private String referenceRange;
    private String interpretation;
    private String participant;
}
