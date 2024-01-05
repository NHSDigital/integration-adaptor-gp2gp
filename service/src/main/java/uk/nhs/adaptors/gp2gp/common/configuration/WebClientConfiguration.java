package uk.nhs.adaptors.gp2gp.common.configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.convert.DurationUnit;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebClientConfiguration {
    private int maxBackoffAttempts;
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration minBackOff;
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout;
}
