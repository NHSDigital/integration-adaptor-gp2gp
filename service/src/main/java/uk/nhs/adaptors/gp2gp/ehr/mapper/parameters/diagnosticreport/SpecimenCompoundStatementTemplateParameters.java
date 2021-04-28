package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SpecimenCompoundStatementTemplateParameters {
    private String compoundStatementId;
    private String diagnosticReportIssuedDate;
    private String accessionIdentifier;
    private String effectiveTime;
    private String type;
    private String observations;
}
