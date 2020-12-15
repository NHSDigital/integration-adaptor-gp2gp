package uk.nhs.adaptors.gp2gp.common.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class IdGenerationService {
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
