package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

    private static final String FILE_EXPECTED_DOWNLOAD = "src/intTest/resources/downloads/test.txt";
    private static final String SETUP_FILE_TXT = "setupFile.txt";
    private static File upload;

    @BeforeEach
    public void setupConnector() throws IOException {
        String fileToUpload = StorageConnectorTest.class.getResource("/test.txt").getPath();
        upload = new File(fileToUpload);
        InputStream inputStream = FileUtils.openInputStream(upload);
        storageConnector.uploadToStorage(inputStream, inputStream.available(), SETUP_FILE_TXT);
    }

    @Test
    public void When_DownloadingFileFromStorage_Expect_FileContentToBeCorrect() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(storageConnector.downloadFromStorage(SETUP_FILE_TXT).readAllBytes());
        File download = new File(FILE_EXPECTED_DOWNLOAD);
        FileUtils.copyToFile(inputStream, download);

        assertThat(FileUtils.contentEquals(upload, download), is(true));
    }

    @AfterAll
    public static void deleteDownloadedFile() {
        new File(FILE_EXPECTED_DOWNLOAD).delete();
    }
}
