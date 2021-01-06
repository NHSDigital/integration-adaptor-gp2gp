package uk.nhs.adaptors.gp2gp.common.service;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TimestampService {
    public Instant now() {
        return Instant.now();
    }
}
