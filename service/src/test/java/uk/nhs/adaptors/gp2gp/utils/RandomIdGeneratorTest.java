package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class RandomIdGeneratorTest {

    @Test
    public void When_GeneratingRandomId_Expect_GeneratedIdIsRandomUUID() {
        String id1 = new RandomIdGenerator().createNewId();
        String id2 = new RandomIdGenerator().createNewId();
        assertThatCode(() -> UUID.fromString(id1))
            .doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(id2))
            .doesNotThrowAnyException();
        assertThat(id1).isNotEqualTo(id2);
    }

}