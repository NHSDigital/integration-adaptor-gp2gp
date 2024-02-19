package uk.nhs.adaptors.gp2gp.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class RandomIdGeneratorServiceTest {

    private static final String UUID_UPPERCASE_REGEXP = "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}";

    @Test
    public void When_CreatingNewId_Expect_GeneratedIdIsRandomUUID() {
        String id1 = new RandomIdGeneratorService().createNewId();
        String id2 = new RandomIdGeneratorService().createNewId();

        assertAll(
            () -> assertThat(id1).isNotEqualTo(id2),
            () -> assertThat(id1).matches(UUID_UPPERCASE_REGEXP),
            () -> assertThat(id2).matches(UUID_UPPERCASE_REGEXP)
        );
    }

    @Test
    public void When_GeneratingIdFromExistingId_And_IdIsAValidUUID_Expect_ThatUUIDIsUsed() {
        var UUIDString = UUID.randomUUID().toString();

        var generatedUUID = new RandomIdGeneratorService().createNewOrUseExistingUUID(UUIDString);

        assertThat(generatedUUID).isEqualTo(UUIDString.toUpperCase());
    }

    @Test
    public void When_GeneratingIdFromExistingId_And_IdIsNotAValidUUID_Expect_NewUUIDIsGenerated() {
        var idString = "THIS-IS-NOT-A-VALID-GUID";

        var generatedUUID = new RandomIdGeneratorService().createNewOrUseExistingUUID(idString);

        assertAll(
                () -> assertThat(generatedUUID).isNotEqualTo(idString),
                () -> assertThat(generatedUUID).matches(UUID_UPPERCASE_REGEXP)
        );
    }
}