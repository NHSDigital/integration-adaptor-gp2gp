package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DiagnosticReportCompoundStatementTemplateParameters {
    private String compoundStatementId;
}
