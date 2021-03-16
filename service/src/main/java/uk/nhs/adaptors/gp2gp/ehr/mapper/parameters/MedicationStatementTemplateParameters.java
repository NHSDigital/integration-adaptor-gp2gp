package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MedicationStatementTemplateParameters {
    private String moodCode;
    private String medicationStatementId;
    private String statusCode;

    private String quantityValue;
    private String quantityText;

    private String medicationStatementPertinentInformation;
    private String ehrSupplyPertinentInformation;

    private String ehrSupplyDiscontinueId;
    private String ehrSupplyDiscontinueCode;
    private String ehrSupplyDiscontinueAvailabilityTime;

    private String effectiveTime;
    private String availabilityTime;

    private boolean hasPriorPrescription;
}
