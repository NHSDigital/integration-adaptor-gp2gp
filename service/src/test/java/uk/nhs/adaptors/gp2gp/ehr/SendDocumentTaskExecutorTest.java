package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendDocumentTaskExecutorTest {
    @ParameterizedTest
    @MethodSource("chunkTestData")
    void When_ChunkingString_Expect_StringIsProperlySplit(String input, int sizeThreshold, List<String> output) {
        var result = SendDocumentTaskExecutor.chunkBinary(input, sizeThreshold);
        assertThat(result).containsExactlyElementsOf(output);
    }

    static Stream<Arguments> chunkTestData() {
        return Stream.of(
            Arguments.of("ℤ qwe asd zxc", 5, List.of("ℤ q", "we as", "d zxc")),
            Arguments.of("ℤ qwe ", 5, List.of("ℤ q", "we ")),
            Arguments.of("ℤ q ℤ", 5, List.of("ℤ q", " ℤ")),
            Arguments.of("ℤ ℤ ℤ", 5, List.of("ℤ ", "ℤ ", "ℤ")),
            Arguments.of("  ℤℤℤ  ", 5, List.of("  ℤ", "ℤ", "ℤ  ")),
            Arguments.of("ℤ  ℤ", 8, List.of("ℤ  ℤ")),
            Arguments.of("ℤ  ℤ", 7, List.of("ℤ  ", "ℤ"))
        );
    }

    @Test
    void When_ChunkingUsingLessThan5SizeThreshold_Expect_Exception() {
        assertThatThrownBy(() -> SendDocumentTaskExecutor.chunkBinary("", 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SizeThreshold must be larger 4 to hold at least 1 UTF-16 character");
    }
}
