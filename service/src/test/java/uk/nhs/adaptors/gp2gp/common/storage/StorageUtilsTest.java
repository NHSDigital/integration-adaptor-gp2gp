package uk.nhs.adaptors.gp2gp.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

public class StorageUtilsTest {

    @Test
    public void When_FileInputSizeIsNine_Then_ReturnNine() throws IOException {
        StorageUtils storageUtils = new StorageUtils();
        String test = "123456789";
        InputStream is = new ByteArrayInputStream(test.getBytes());
        var number = storageUtils.getInputStreamSize(is);

        assertThat(number).isEqualTo(test.length());
    }
}
