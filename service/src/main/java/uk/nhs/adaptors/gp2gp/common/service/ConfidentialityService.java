package uk.nhs.adaptors.gp2gp.common.service;

import lombok.RequiredArgsConstructor;
import org.hl7.fhir.dstu3.model.Coding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ConfidentialityService {
    private static final String REDACTION_INTERACTION_ID = "RCMR_IN030000UK07";
    private static final String REDACTION_CODE_SYSTEM = "http://hl7.org/fhir/v3/ActCode";
    public static final String NOPAT = "NOPAT";
    private static final String REDACTION_CONFIDENTIALITY_CODE = """
        <confidentialityCode
            code="NOPAT"
            codeSystem="2.16.840.1.113883.4.642.3.47"
            displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
        />""";

    private final RedactionsContext redactionsContext;

    public Optional<String> generateConfidentialityCode(List<Coding> metaSecurity) {
        return isRedactionMessage() && hasNOPATMetaSecurity(metaSecurity)
            ? Optional.of(REDACTION_CONFIDENTIALITY_CODE)
            : Optional.empty();
    }

    private boolean isRedactionMessage() {
        return REDACTION_INTERACTION_ID.equals(redactionsContext.ehrExtractInteractionId());
    }

    private boolean isNOPATCoding(Coding coding) {
        return NOPAT.equals(coding.getCode()) && REDACTION_CODE_SYSTEM.equals(coding.getSystem());
    }

    private boolean hasNOPATMetaSecurity(List<Coding> metaSecurity) {
        return metaSecurity
            .stream()
            .anyMatch(this::isNOPATCoding);
    }
}
