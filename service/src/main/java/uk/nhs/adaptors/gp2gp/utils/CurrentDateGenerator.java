package uk.nhs.adaptors.gp2gp.utils;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class CurrentDateGenerator {
    public LocalDateTime generateDate() {
        return LocalDateTime.now();
    }
}
