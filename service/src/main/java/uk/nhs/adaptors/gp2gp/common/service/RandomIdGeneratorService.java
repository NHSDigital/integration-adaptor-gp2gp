package uk.nhs.adaptors.gp2gp.common.service;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class RandomIdGeneratorService {

    private static final Pattern UUID_REGEX =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public String createNewId() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    public String createNewOrUseExistingUUID(String id) {
        return UUID_REGEX.matcher(id).matches()
                ? id.toUpperCase()
                : UUID.randomUUID().toString().toUpperCase();

    }
}
