package uk.nhs.adaptors.gp2gp.common.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Gzip {
    public static byte[] compress(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(content.getBytes(UTF_8));
            gzipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while compressing content", e);
        }
    }

    public static String decompress(byte[] compressedContent) {
        if (compressedContent == null || compressedContent.length == 0) {
            return null;
        }
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedContent));
             StringWriter stringWriter = new StringWriter()) {
            IOUtils.copy(gzipInputStream, stringWriter, UTF_8);
            return stringWriter.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while decompressing content", e);
        }
    }
}
