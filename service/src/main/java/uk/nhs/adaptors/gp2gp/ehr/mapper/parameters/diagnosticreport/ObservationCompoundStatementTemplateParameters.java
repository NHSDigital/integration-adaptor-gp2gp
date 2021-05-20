package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ObservationCompoundStatementTemplateParameters {
    private String classCode;
    private String compoundStatementId;
    private String codeElement;
    private String effectiveTime;
    private String availabilityTimeElement;
    private String interpretation;
    private String participant;
    private String observationStatement;
    private String narrativeStatements;
    private String statementsForDerivedObservations;
}
