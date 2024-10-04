package uk.nhs.adaptors.gp2gp.common.service;

import lombok.RequiredArgsConstructor;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;

import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ConfidentialityService {
    private static final String REDACTION_CODE_SYSTEM = "http://hl7.org/fhir/v3/ActCode";
    public static final String NOPAT = "NOPAT";
    private static final String REDACTION_CONFIDENTIALITY_CODE = """
        <confidentialityCode
            code="NOPAT"
            codeSystem="2.16.840.1.113883.4.642.3.47"
            displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
        />""";

    private final RedactionsContext redactionsContext;

    public Optional<String> generateConfidentialityCode(Resource resource) {
        return redactionsContext.isRedactionMessage()
               && (hasNOPATMetaSecurity(resource) || hasNOPATSecurityLabel(resource))
               ? Optional.of(REDACTION_CONFIDENTIALITY_CODE)
               : Optional.empty();
    }

    private boolean isNOPATCoding(Coding coding) {
        return NOPAT.equals(coding.getCode()) && REDACTION_CODE_SYSTEM.equals(coding.getSystem());
    }

    private boolean hasNOPATMetaSecurity(Resource resource) {
        return resource.getMeta().getSecurity()
            .stream()
            .anyMatch(this::isNOPATCoding);
    }

    public boolean hasNOPATSecurityLabel(Resource resource) {
        return (resource instanceof DocumentReference documentReference)
               && documentReference
                   .getSecurityLabel()
                   .stream()
                   .anyMatch(codeableConcept -> codeableConcept.getCoding().stream().anyMatch(this::isNOPATCoding));
    }
}
