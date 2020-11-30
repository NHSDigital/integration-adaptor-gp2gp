package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class S3StorageConnectorTest {
    private static StorageConnector storageConnector;
    private static final String FILE_TO_UPLOAD = "src/test/resources/test.txt";
    private static final String FILE_EXPECTED_DOWNLOAD = "src/intTest/resources/downloads/test.txt";
    private static final String SETUP_FILE = "setupFile.txt";
    private static final String TEST_UPLOAD = "uploadFile.txt";

    @BeforeAll
    public static void setupConnector() throws IOException {
        storageConnector = new S3StorageConnector();
        File file = new File(FILE_TO_UPLOAD);
        InputStream inputStream = FileUtils.openInputStream(file);
        storageConnector.uploadToStorage(inputStream, SETUP_FILE);
    }

    @Test
    public void uploadToS3Storage() throws IOException {
        List<String> files = storageConnector.getFileListFromStorage();
        int count = files.size();
        File upload = new File(FILE_TO_UPLOAD);
        InputStream inputStream = new FileInputStream(upload);
        storageConnector.uploadToStorage(inputStream, TEST_UPLOAD);
        count++;
        files = storageConnector.getFileListFromStorage();
        assertTrue(files.contains(TEST_UPLOAD));
        assertEquals(count, files.size());
    }

    @Test
    public void downloadFromS3Storage() throws IOException {
        List<String> files = storageConnector.getFileListFromStorage();
        assertTrue(files.contains(SETUP_FILE));
        OutputStream fileFromStorage = storageConnector.downloadFromStorage(SETUP_FILE);
        ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) fileFromStorage;
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        File upload = new File(FILE_TO_UPLOAD);
        File expectedDownload = new File(FILE_EXPECTED_DOWNLOAD);
        FileUtils.copyToFile(inputStream, expectedDownload);
        assertEquals(FileUtils.readLines(upload), FileUtils.readLines(expectedDownload));
    }

    @AfterAll
    public static void deleteDownloadedFile() {
        File fileToDelete = new File(FILE_EXPECTED_DOWNLOAD);
        fileToDelete.delete();
    }
}
