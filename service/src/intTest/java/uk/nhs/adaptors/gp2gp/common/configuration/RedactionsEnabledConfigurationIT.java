package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { "gp2gp.redactions-enabled = true" })
@ContextConfiguration(classes = { RedactionsConfiguration.class })
class RedactionsEnabledConfigurationIT {
    private static final String EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS = "RCMR_IN030000UK07";

    @Autowired
    private RedactionsContext redactionsContext;

    @Test
    void When_RedactionsEnabled_Expect_EhrExtractInteractionIdAsUK07() {
        final String result = redactionsContext.ehrExtractInteractionId();
        assertThat(result).isEqualTo(EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS);
    }
}
