package uk.nhs.adaptors.gp2gp;

import java.util.UUID;

public class IdGenerator {
    public static String get() {
        return UUID.randomUUID().toString();
    }
}
