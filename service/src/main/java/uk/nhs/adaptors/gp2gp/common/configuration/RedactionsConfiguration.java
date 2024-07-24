package uk.nhs.adaptors.gp2gp.common.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RedactionsConfiguration {
    public static final String EHR_EXTRACT_INTERACTION_ID = "RCMR_IN030000UK06";
    public static final String EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS = "RCMR_IN030000UK07";
    private boolean redactionsEnabled;

    @Value("${gp2gp.redactions-enabled}")
    protected void setRedactionsEnabled(boolean redactionsEnabled) {
        this.redactionsEnabled = redactionsEnabled;
    }

    @Bean
    RedactionsContext redactionsContext() {
        final String ehrExtractInteractionId = redactionsEnabled
            ? EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS
            : EHR_EXTRACT_INTERACTION_ID;

        return new RedactionsContext(ehrExtractInteractionId);
    }
}