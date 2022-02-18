package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SendDocumentTaskExecutorTest {

    private static final int SIZE_THRESHOLD_FOUR = 4;

    @ParameterizedTest
    @MethodSource("chunkTestData")
    void When_ChunkingString_Expect_StringIsProperlySplit(String input, int sizeThreshold, List<String> output) {
        var result = SendDocumentTaskExecutor.chunkBinary(input, sizeThreshold);
        assertThat(result).containsExactlyElementsOf(output);
    }

    static Stream<Arguments> chunkTestData() {
        return Stream.of(
            Arguments.of("QWER1234", SIZE_THRESHOLD_FOUR, List.of("QWER", "1234")),
            Arguments.of("QWER", SIZE_THRESHOLD_FOUR, List.of("QWER")),
            Arguments.of("QWE", SIZE_THRESHOLD_FOUR, List.of("QWE")),
            Arguments.of("QWER12", SIZE_THRESHOLD_FOUR, List.of("QWER", "12"))
        );
    }

//    @Test
//    void When_ChunkingLarge_Expect_NoError() throws IOException {
//        var filePath = "/Users/bartoszs/Downloads/FA983859-6271-4B56-9897-EE013047676E.txt";
//
//        String binary = new String(Files.readAllBytes( Paths.get(filePath)));
//
//        var chunks = SendDocumentTaskExecutor.chunkBinary(binary, 4_500_000);
//        var a = 1;
//    }
}
