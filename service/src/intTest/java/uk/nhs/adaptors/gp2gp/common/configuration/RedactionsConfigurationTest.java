package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = { RedactionsConfiguration.class })
public class RedactionsConfigurationTest {
    @Nested
    @SpringBootTest(properties = { "gp2gp.redactions-enabled = false" })
    class FeatureFlagDisabled {
        @Autowired
        private RedactionsContext redactionsContext;

        @Test
        void ehrExtractInteractionIdIsUK07() {
            final String result = redactionsContext.ehrExtractInteractionId();
            assertThat(result).isEqualTo(RedactionsConfiguration.EHR_EXTRACT_INTERACTION_ID);
        }
    }

    @Nested
    @SpringBootTest(properties = { "gp2gp.redactions-enabled = true" })
    class FeatureFlagEnabled {
        @Autowired
        private RedactionsContext redactionsContext;

        @Test
        void ehrExtractInteractionIdIsUK07() {
            final String result = redactionsContext.ehrExtractInteractionId();
            assertThat(result).isEqualTo(RedactionsConfiguration.EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS);
        }
    }
}
