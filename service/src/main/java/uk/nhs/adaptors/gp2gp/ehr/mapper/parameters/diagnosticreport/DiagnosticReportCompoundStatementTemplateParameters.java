package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DiagnosticReportCompoundStatementTemplateParameters {
    private String compoundStatementId;
    private String diagnosticReportIssuedDate;
    private String specimens;
}
