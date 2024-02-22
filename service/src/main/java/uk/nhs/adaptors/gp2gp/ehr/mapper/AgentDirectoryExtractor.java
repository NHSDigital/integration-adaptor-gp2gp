package uk.nhs.adaptors.gp2gp.ehr.mapper;


import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentDirectoryExtractor {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";

    public static Optional<Patient> extractPatientByNhsNumber(Bundle bundle, String nhsNumber) {
        return ResourceExtractor.extractResourcesByType(bundle, Patient.class)
            .filter(patient -> isNhsNumberMatching(patient, nhsNumber))
            .findFirst();
    }

    private static boolean isNhsNumberMatching(Patient patient, String nhsNumber) {
        return patient.getIdentifier()
            .stream()
            .filter(identifier -> identifier.getSystem().equals(NHS_NUMBER_SYSTEM))
            .anyMatch(identifier -> identifier.getValue().equals(nhsNumber));
    }

    @Data
    public static class AgentData {
        private final Practitioner practitioner;
        private final PractitionerRole practitionerRole;
        private final Organization organization;
    }
}
