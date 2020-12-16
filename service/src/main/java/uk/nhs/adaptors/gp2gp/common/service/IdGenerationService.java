package uk.nhs.adaptors.gp2gp.common.service;

import java.util.UUID;

public class IdGenerationService {
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
