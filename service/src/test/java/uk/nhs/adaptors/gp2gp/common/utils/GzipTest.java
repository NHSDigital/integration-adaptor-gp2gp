package uk.nhs.adaptors.gp2gp.common.utils;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class GzipTest {

    @Test
    void When_CompressingText_Expect_GzipIsCreated() {
        var original = "some_test_content";
        var compressed = Gzip.compress(original);

        assertThat(compressed).isNotNull();
        assertThat(new String(compressed, UTF_8)).isNotEqualTo(original);

        var decompressed = Gzip.decompress(compressed);

        assertThat(decompressed).isEqualTo(original);
    }

    @Test
    void When_CompressingEmptyString_Expect_NullIsReturned() {
        assertThat(Gzip.compress("")).isNull();
    }

    @Test
    void When_CompressingNull_Expect_NullIsReturned() {
        assertThat(Gzip.compress(null)).isNull();
    }
}
