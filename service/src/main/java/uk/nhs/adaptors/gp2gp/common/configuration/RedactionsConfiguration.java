package uk.nhs.adaptors.gp2gp.common.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class RedactionsConfiguration {
    private boolean redactionsEnabled;

    protected RedactionsConfiguration(@Value("${gp2gp.redactions-enabled}") boolean redactionsEnabled) {
        this.redactionsEnabled = redactionsEnabled;
    }
}