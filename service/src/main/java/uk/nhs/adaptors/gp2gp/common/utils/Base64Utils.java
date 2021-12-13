package uk.nhs.adaptors.gp2gp.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;

public class Base64Utils {

    public static String toBase64String(String nonBase64FileContent) {
        return new String(encodeToBase64(nonBase64FileContent), UTF_8);
    }

    public static int toBase64ByteLength(String nonBase64FileContent) {
        return encodeToBase64(nonBase64FileContent).length;
    }

    private static byte[] encodeToBase64(String nonBase64FileContent) {
        return Base64.getEncoder().encode(nonBase64FileContent.getBytes(UTF_8));
    }

}
