package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;

public class MedicationRequestUtils {
    public static boolean isStoppedMedicationOrder(MedicationRequest medicationRequest) {
        return medicationRequest.hasStatus() && medicationRequest.hasIntent()
            && medicationRequest.getStatus().equals(MedicationRequest.MedicationRequestStatus.STOPPED)
            && medicationRequest.getIntent().equals(MedicationRequest.MedicationRequestIntent.ORDER);
    }

    public static boolean isMedicationRequestType(Reference reference) {
        return reference.getReferenceElement().getResourceType()
            .equals(ResourceType.MedicationRequest.toString());
    }
}
