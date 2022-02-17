//package uk.nhs.adaptors.gp2gp.ehr;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.stream.Stream;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//class SendDocumentTaskExecutorTest {
//
//    private static final int SIZE_THRESHOLD_FOUR = 4;
//    private static final int SIZE_THRESHOLD_FIVE = 5;
//    private static final int SIZE_THRESHOLD_SEVEN = 7;
//    private static final int SIZE_THRESHOLD_EIGHT = 8;
//
//    @ParameterizedTest
//    @MethodSource("chunkTestData")
//    void When_ChunkingString_Expect_StringIsProperlySplit(String input, int sizeThreshold, List<String> output) {
//        var result = SendDocumentTaskExecutor.chunkBinary(input, sizeThreshold);
//        assertThat(result).containsExactlyElementsOf(output);
//    }
//
//    static Stream<Arguments> chunkTestData() {
//        return Stream.of(
//            Arguments.of("ℤ qwe asd zxc", SIZE_THRESHOLD_FIVE, List.of("ℤ q", "we as", "d zxc")),
//            Arguments.of("ℤ qwe ", SIZE_THRESHOLD_FIVE, List.of("ℤ q", "we ")),
//            Arguments.of("ℤ q ℤ", SIZE_THRESHOLD_FIVE, List.of("ℤ q", " ℤ")),
//            Arguments.of("ℤ ℤ ℤ", SIZE_THRESHOLD_FIVE, List.of("ℤ ", "ℤ ", "ℤ")),
//            Arguments.of("  ℤℤℤ  ", SIZE_THRESHOLD_FIVE, List.of("  ℤ", "ℤ", "ℤ  ")),
//            Arguments.of("ℤ  ℤ", SIZE_THRESHOLD_EIGHT, List.of("ℤ  ℤ")),
//            Arguments.of("ℤ  ℤ", SIZE_THRESHOLD_SEVEN, List.of("ℤ  ", "ℤ"))
//        );
//    }
//
//    @Test
//    void When_ChunkingUsingLessThan5SizeThreshold_Expect_Exception() {
//        assertThatThrownBy(() -> SendDocumentTaskExecutor.chunkBinary("", SIZE_THRESHOLD_FOUR))
//            .isInstanceOf(IllegalArgumentException.class)
//            .hasMessage("SizeThreshold must be larger 4 to hold at least 1 UTF-16 character");
//    }
//
//    @Test
//    void When_ChunkingLarge_Expect_NoError() throws IOException {
//        var filePath = "/Users/bartoszs/Downloads/FA983859-6271-4B56-9897-EE013047676E.txt";
//
//        String binary = new String(Files.readAllBytes( Paths.get(filePath)));
//
//        var chunks = SendDocumentTaskExecutor.chunkBinary(binary, 4_500_000);
//        var a = 1;
//    }
//}
