package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext.NON_REDACTION_INTERACTION_ID;
import static uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext.REDACTION_INTERACTION_ID;

public class RedactionsContextTest {

    @Test
    public void When_IsRedactionMessageAndInteractionIdIsRCMRIN030000UK07_Expect_IsRedactionMessage() {
        final var redactionsContext = new RedactionsContext(REDACTION_INTERACTION_ID);

        final var isRedactionMessage = redactionsContext.isRedactionMessage();

        assertThat(isRedactionMessage).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { NON_REDACTION_INTERACTION_ID })
    @NullAndEmptySource
    public void When_IsRedactionMessageAndInteractionIdIsNotRCMRIN030000UK07_Expect_IsNotRedactionMessage(
        String interactionId
    ) {
        final var redactionsContext = new RedactionsContext(interactionId);

        final var isRedactionMessage = redactionsContext.isRedactionMessage();

        assertThat(isRedactionMessage).isFalse();
    }
}
