package uk.nhs.adaptors.gp2gp.common.service;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext.NON_REDACTION_INTERACTION_ID;
import static uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext.REDACTION_INTERACTION_ID;

@ExtendWith(MockitoExtension.class)
public class ConfidentialityServiceTest {
    private static final Meta META_WITH_NO_SECURITY = new Meta();
    private static final Meta META_WITH_NOPAT_SECURITY = new Meta()
        .addSecurity(
            "http://hl7.org/fhir/v3/ActCode",
            "NOPAT",
            "no disclosure to patient, family or caregivers without attending provider's authorization"
        );
    private static final Meta META_WITH_NON_NOPAT_SECURITY = new Meta()
        .addSecurity(
            "http://hl7.org/fhir/v3/ActCode",
            "NON-NOPAT",
            "random disclosure to patient, family or caregivers without attending provider's authorization"
        );

    private static Stream<Arguments> When_GenerateAndIsNotRedactionMessage_Expect_EmptyOptional() {
        return Stream.of(
            arguments(named("Meta with no security", META_WITH_NO_SECURITY)),
            arguments(named("Meta with non-NOPAT security", META_WITH_NON_NOPAT_SECURITY)),
            arguments(named("Meta with NOPAT security", META_WITH_NOPAT_SECURITY))
        );
    }

    @ParameterizedTest
    @MethodSource
    @NullSource
    public void When_GenerateAndIsNotRedactionMessage_Expect_EmptyOptional(Meta meta) {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(NON_REDACTION_INTERACTION_ID)
        );
        var resource = new Observation().setMeta(meta);

        var confidentialityCode = confidentialityService.generateConfidentialityCode(resource);

        assertThat(confidentialityCode)
            .isEmpty();
    }

    @Test
    public void When_GenerateAndIsRedactionMessageAndNoMetaSecurityIsPresent_Expect_EmptyOptional() {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(REDACTION_INTERACTION_ID)
        );
        var resource = new AllergyIntolerance().setMeta(META_WITH_NO_SECURITY);

        var confidentialityCode = confidentialityService.generateConfidentialityCode(resource);

        assertThat(confidentialityCode)
            .isEmpty();
    }

    @Test
    public void When_GenerateAndIsRedactionMessageAndNonNOPATMetaSecurityIsPresent_Expect_EmptyOptional() {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(REDACTION_INTERACTION_ID)
        );
        var resource = new ReferralRequest().setMeta(META_WITH_NON_NOPAT_SECURITY);

        var confidentialityCode = confidentialityService.generateConfidentialityCode(resource);

        assertThat(confidentialityCode)
            .isEmpty();
    }

    @Test
    public void When_GenerateAndIsRedactionMessageAndNOPATMetaSecurityIsPresent_Expect_ConfidentialityCode() {
        var confidentialityService = new ConfidentialityService(
            new RedactionsContext(REDACTION_INTERACTION_ID)
        );
        var resource = new Immunization().setMeta(META_WITH_NOPAT_SECURITY);

        var confidentialityCode = confidentialityService.generateConfidentialityCode(resource);

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
