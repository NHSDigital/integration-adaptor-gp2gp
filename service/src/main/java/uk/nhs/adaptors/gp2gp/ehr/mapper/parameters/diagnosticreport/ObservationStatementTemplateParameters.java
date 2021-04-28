package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

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
    private String availabilityTime;
    private String value;
    private String referenceRange;
    private String interpretation;
    private String participant;
}
