package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MedicationStatementTemplateParameters {
    private String medicationStatementId;
    private String statusCode;
    private String effectiveTime;
    private String availabilityTime;
    private String confidentialityCode;
    private String medicationReferenceCode;
    private String ehrSupplyId;
    private String medicationStatementPertinentInformation;
    private String ehrSupplyPertinentInformation;
    private String quantityValue;
    private String quantityText;
    private String repeatNumber;
    private boolean hasEhrSupplyDiscontinue;
    private String ehrSupplyDiscontinueId;
    private String ehrSupplyDiscontinueCode;
    private String ehrSupplyDiscontinueAvailabilityTime;
    private String ehrSupplyDiscontinueReasonText;
    private String basedOn;
    private String participant;
    private String ehrSupplyTypeCode;
}
