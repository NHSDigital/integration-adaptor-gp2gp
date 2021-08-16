package uk.nhs.adaptors.gp2gp.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StringChunking {

    private static final int THRESHOLD_MINIMUM = 4;

    public static List<String> chunkEhrExtract(String ehrExtract, int sizeThreshold) {
        if (sizeThreshold <= THRESHOLD_MINIMUM) {
            throw new IllegalArgumentException("SizeThreshold must be larger 4 to hold at least 1 UTF-16 character");
        }

        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < ehrExtract.length(); i++) {
            var c = ehrExtract.charAt(i);
            var chunkBytesSize = chunk.toString().getBytes(StandardCharsets.UTF_8).length;
            var charBytesSize = Character.toString(c).getBytes(StandardCharsets.UTF_8).length;
            if (chunkBytesSize + charBytesSize > sizeThreshold) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(c);
        }
        if (chunk.length() != 0) {
            chunks.add(chunk.toString());
        }

        return chunks;
    }

    public static int getBytesLengthOfString(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }

}
