package uk.nhs.adaptors.gp2gp.utils;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RandomIdGenerator {
    public String createNewId() {
        return UUID.randomUUID().toString();
    }
}
