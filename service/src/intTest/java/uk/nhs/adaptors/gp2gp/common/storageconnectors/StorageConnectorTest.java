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
    private static final File upload = loadUploadFromFilePath();
    private static final File download = loadDownloadFromFilePath();

    @BeforeEach
    public void setupConnector() throws IOException {
        InputStream inputStream = FileUtils.openInputStream(upload);
        storageConnector.uploadToStorage(inputStream, SETUP_FILE);
    }

    @Test
    public void When_DownloadingFileFromStorage_ExpectFileContentToBeCorrect() throws IOException {
        OutputStream fileFromStorage = storageConnector.downloadFromStorage(SETUP_FILE);
        ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) fileFromStorage;
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        FileUtils.copyToFile(inputStream, download);
        assertEquals(FileUtils.readLines(upload), FileUtils.readLines(download));
    }

    @AfterAll
    public static void deleteDownloadedFile() {
        new File(FILE_EXPECTED_DOWNLOAD).delete();
    }

    private static File loadUploadFromFilePath() {
        return new File(FILE_TO_UPLOAD);
    }

    private static File loadDownloadFromFilePath() {
        return new File(FILE_EXPECTED_DOWNLOAD);
    }
}
