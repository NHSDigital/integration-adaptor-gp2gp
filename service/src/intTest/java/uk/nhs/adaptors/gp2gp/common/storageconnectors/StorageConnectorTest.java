package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class StorageConnectorTest {

    @Autowired
    private StorageConnector storageConnector;

    private static final String FILE_TO_UPLOAD = "src/intTest/resources/test.txt";
    private static final String FILE_EXPECTED_DOWNLOAD = "src/intTest/resources/downloads/test.txt";
    private static final String SETUP_FILE = "setupFile.txt";

    @BeforeEach
    public void setupConnector() throws IOException {
        File file = getFileToUpload();
        InputStream inputStream = FileUtils.openInputStream(file);
        storageConnector.uploadToStorage(inputStream, SETUP_FILE);
    }

    @Test
    public void When_DownloadingFileFromStorage_ExpectFileContentToBeCorrect() throws IOException {
        File upload = getFileToUpload();
        File expectedDownload = new File(FILE_EXPECTED_DOWNLOAD);

        OutputStream fileFromStorage = storageConnector.downloadFromStorage(SETUP_FILE);
        ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) fileFromStorage;
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        FileUtils.copyToFile(inputStream, expectedDownload);
        assertEquals(FileUtils.readLines(upload), FileUtils.readLines(expectedDownload));
    }

    @AfterAll
    public static void deleteDownloadedFile() {
        new File(FILE_EXPECTED_DOWNLOAD).delete();
    }

    private static File getFileToUpload() {
        return new File(FILE_TO_UPLOAD);
    }
}
