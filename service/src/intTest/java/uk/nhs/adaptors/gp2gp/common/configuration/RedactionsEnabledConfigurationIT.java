package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@SpringBootTest(properties = { "gp2gp.redactions-enabled = true" })
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
