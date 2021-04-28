package uk.nhs.adaptors.gp2gp.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class RandomIdGeneratorServiceTest {

    private static final String UUID_UPPERCASE_REGEXP = "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}";

    @Test
    public void When_GeneratingRandomId_Expect_GeneratedIdIsRandomUUID() {
        String id1 = new RandomIdGeneratorService().createNewId();
        String id2 = new RandomIdGeneratorService().createNewId();

        assertAll(
            () -> assertThatCode(() -> UUID.fromString(id1)).doesNotThrowAnyException(),
            () -> assertThatCode(() -> UUID.fromString(id2)).doesNotThrowAnyException(),
            () -> assertThat(id1).isNotEqualTo(id2),
            () -> assertThat(id1).matches(UUID_UPPERCASE_REGEXP),
            () -> assertThat(id2).matches(UUID_UPPERCASE_REGEXP)
        );
    }
}