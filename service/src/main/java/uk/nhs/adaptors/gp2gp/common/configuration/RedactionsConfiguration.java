package uk.nhs.adaptors.gp2gp.common.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class RedactionsConfiguration {
    private boolean redactionsEnabled;

    @Value("${gp2gp.redactions-enabled}")
    public void setRedactionsEnabled(boolean redactionsEnabled) {
        this.redactionsEnabled = redactionsEnabled;
    }
}