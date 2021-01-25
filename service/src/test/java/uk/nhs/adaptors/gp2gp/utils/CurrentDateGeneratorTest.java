package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class CurrentDateGeneratorTest {
    @Test
    public void When_GeneratingCurrentDate_Expect_LocalDateProperlyGenerated() {
        LocalDateTime date1 = new CurrentDateGenerator().generateDate();
        LocalDateTime date2 = new CurrentDateGenerator().generateDate();
        assertThat(date1).isNotEqualTo(date2);
    }
}
