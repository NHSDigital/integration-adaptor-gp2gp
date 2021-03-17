package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.MedicationRequest;

public class MedicationStatementExtractor {
    private static final String PRESCRIPTION_TYPE_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-PrescriptionType-1";
    private static final String REPEAT_INFORMATION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationRepeatInformation-1";
    private static final String NUM_OF_REPEAT_PRESCRIPTIONS_ALLOWED_URL = "numberOfRepeatPrescriptionsAllowed";
    private static final String DEFAULT_NON_ACUTE_REPEAT_VALUE = "1";

    public static String extractPrescriptionTypeCode (MedicationRequest medicationRequest) {
        return medicationRequest.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(PRESCRIPTION_TYPE_URL))
            .findFirst()
            .map(value -> (CodeableConcept) value.getValue())
            .map(code -> code.getCodingFirstRep().getCode())
            .orElse(StringUtils.EMPTY);
    }

    public static String extractNonAcuteRepeatValue(MedicationRequest medicationRequest) {
        return medicationRequest.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(REPEAT_INFORMATION_URL))
            .findFirst()
            .get() // TODO: refactor
            .getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(NUM_OF_REPEAT_PRESCRIPTIONS_ALLOWED_URL) && value.hasValue())
            .findFirst()
            .map(value -> value.getValue().toString()) // TODO: refactor?
            .orElse(DEFAULT_NON_ACUTE_REPEAT_VALUE);
    }
}
