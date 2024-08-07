package uk.nhs.adaptors.gp2gp.common.service;

import org.hl7.fhir.dstu3.model.Coding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ConfidentialityServiceTest {
    private static final List<Coding> NO_META_SECURITY = List.of(new Coding());
    private static final List<Coding> NOPAT_META_SECURITY = List.of(
        new Coding(
            "http://hl7.org/fhir/v3/ActCode",
            "NOPAT",
            "no disclosure to patient, family or caregivers without attending provider's authorization"
        )
    );
    private static final List<Coding> NON_NOPAT_META_SECURITY = List.of(
        new Coding(
            "http://hl7.org/fhir/v3/ActCode",
            "NON-NOPAT",
            "random disclosure to patient, family or caregivers without attending provider's authorization"
        )
    );
    public static final String NON_REDACTION_INTERACTION_ID = "RCMR_IN030000UK06";
    public static final String REDACTION_INTERACTION_ID = "RCMR_IN030000UK07";

    private static Stream<List<Coding>> When_GenerateAndIsNotRedactionMessage_Expect_EmptyOptional() {
        return Stream.of(NO_META_SECURITY, NON_NOPAT_META_SECURITY, NOPAT_META_SECURITY);
    }

    @ParameterizedTest
    @MethodSource
    public void When_GenerateAndIsNotRedactionMessage_Expect_EmptyOptional(List<Coding> metaSecurityList) {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(NON_REDACTION_INTERACTION_ID)
        );
        var confidentialityCode = confidentialityService.generateConfidentialityCode(metaSecurityList);

        assertThat(confidentialityCode)
            .isEmpty();
    }

    @Test
    public void When_GenerateAndIsRedactionMessageAndNoMetaSecurityIsPresent_Expect_EmptyOptional() {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(REDACTION_INTERACTION_ID)
        );
        var confidentialityCode = confidentialityService.generateConfidentialityCode(NO_META_SECURITY);

        assertThat(confidentialityCode)
            .isEmpty();
    }

    @Test
    public void When_GenerateAndIsRedactionMessageAndNonNOPATMetaSecurityIsPresent_Expect_EmptyOptional() {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(REDACTION_INTERACTION_ID)
        );
        var confidentialityCode = confidentialityService.generateConfidentialityCode(NON_NOPAT_META_SECURITY);

        assertThat(confidentialityCode)
            .isEmpty();
    }

    @Test
    public void When_GenerateAndIsRedactionMessageAndNOPATMetaSecurityIsPresent_Expect_ConfidentialityCode() {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(REDACTION_INTERACTION_ID)
        );
        var confidentialityCode = confidentialityService.generateConfidentialityCode(NOPAT_META_SECURITY);

        assertThat(confidentialityCode)
            .isPresent();
        assertThat(confidentialityCode.get())
            .isEqualTo("""
                <confidentialityCode
                    code="NOPAT"
                    codeSystem="2.16.840.1.113883.4.642.3.47"
                    displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
                />"""
            );
    }
}
