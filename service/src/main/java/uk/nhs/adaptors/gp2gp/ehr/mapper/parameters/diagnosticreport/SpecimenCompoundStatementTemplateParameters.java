package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SpecimenCompoundStatementTemplateParameters {
    private String compoundStatementId;
    private String availabilityTime;
    private String specimenRoleId;
    private String accessionIdentifier;
    private String effectiveTime;
    private String specimenMaterialType;
    private String pertinentInformation;
    private String participant;
    private String observations;
}
