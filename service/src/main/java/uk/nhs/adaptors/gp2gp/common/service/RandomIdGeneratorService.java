package uk.nhs.adaptors.gp2gp.common.service;

import java.util.UUID;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class RandomIdGeneratorService {
    public String createNewId() {
        return UUID.randomUUID().toString().toUpperCase();
    }
}
