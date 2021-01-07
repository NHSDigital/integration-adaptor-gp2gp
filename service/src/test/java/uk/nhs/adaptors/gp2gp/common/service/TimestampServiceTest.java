package uk.nhs.adaptors.gp2gp.common.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoField;

import static org.assertj.core.api.Assertions.assertThat;

public class TimestampServiceTest {

    private static final int MILLIS_PER_MICRO = 1000;

    @Test
    public void When_TimestampIsCreated_Expect_TruncatedToMilliseconds() {
        Instant now = new TimestampService().now();
        assertThat(now.getLong(ChronoField.MICRO_OF_SECOND) % MILLIS_PER_MICRO).isZero();
    }

}
