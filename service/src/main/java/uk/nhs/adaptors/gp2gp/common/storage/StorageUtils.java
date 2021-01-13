package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

@Component
public class StorageUtils {
    public int getInputStreamSize(InputStream inputStream) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int b;
        while ((b = inputStream.read()) != -1) {
            os.write(b);
        }
        return os.size();
    }
}
