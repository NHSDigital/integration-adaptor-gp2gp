package uk.nhs.adaptors.gp2gp.common.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RedactionsConfiguration {
    private boolean redactionsEnabled;

    @Value("${gp2gp.redactions-enabled}")
    protected void setRedactionsEnabled(boolean redactionsEnabled) {
        this.redactionsEnabled = redactionsEnabled;
    }

    @Bean
    RedactionsContext redactionsContext() {
        final String ehrExtractInteractionId = redactionsEnabled
            ? RedactionsContext.REDACTION_INTERACTION_ID
            : RedactionsContext.NON_REDACTION_INTERACTION_ID;

        return new RedactionsContext(ehrExtractInteractionId);
    }
}