package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { "gp2gp.redactions-enabled = false" })
@ContextConfiguration(classes = { RedactionsConfiguration.class })
class RedactionsDisabledConfigurationIT {
    private static final String EHR_EXTRACT_INTERACTION_ID = "RCMR_IN030000UK06";

    @Autowired
    private RedactionsContext redactionsContext;

    @Test
    void When_RedactionsDisabled_Expect_EhrExtractInteractionIdAsUK07() {
        final String result = redactionsContext.ehrExtractInteractionId();
        assertThat(result).isEqualTo(EHR_EXTRACT_INTERACTION_ID);
    }
}