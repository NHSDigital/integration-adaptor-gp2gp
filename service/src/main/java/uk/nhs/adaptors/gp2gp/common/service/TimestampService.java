package uk.nhs.adaptors.gp2gp.common.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

@Service
public class TimestampService {
    public Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
