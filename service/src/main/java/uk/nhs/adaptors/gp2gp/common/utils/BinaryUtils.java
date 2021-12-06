package uk.nhs.adaptors.gp2gp.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BinaryUtils {

    public static int getBytesLengthOfString(String input) {
        return input.getBytes(UTF_8).length;
    }

}
