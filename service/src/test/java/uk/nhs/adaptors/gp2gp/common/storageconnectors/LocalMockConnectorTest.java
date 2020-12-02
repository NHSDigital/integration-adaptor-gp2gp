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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LocalMockConnectorTest {

    private static StorageConnector storageConnector;
    private static final String FILE_TO_UPLOAD = "src/test/resources/test.txt";
    private static final String FILE_EXPECTED_DOWNLOAD = "src/test/resources/downloads/test.txt";
    private static final String SETUP_FILE = "setupFile.txt";

    @BeforeAll
    public static void setupConnector() throws IOException {
        storageConnector = new LocalMockConnector();
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
