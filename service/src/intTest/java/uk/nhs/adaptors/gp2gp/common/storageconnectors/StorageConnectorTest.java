package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.nhs.adaptors.gp2gp.extension.IntegrationTestsExtension;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith({ SpringExtension.class, IntegrationTestsExtension.class })
public class StorageConnectorTest {
    @Autowired
    private StorageConnector storageConnector;

    private static final String FILE_EXPECTED_DOWNLOAD = "src/intTest/resources/downloads/test.txt";
    private static final String SETUP_FILE_TXT = "setupFile.txt";
    private File uploadedFile;
    private File downloadedFile;

    @BeforeEach
    public void setupConnector() throws IOException {
        String fileToUpload = StorageConnectorTest.class.getResource("/test.txt").getPath();
        uploadedFile = new File(fileToUpload);
        InputStream inputStream = FileUtils.openInputStream(uploadedFile);
        storageConnector.uploadToStorage(inputStream, inputStream.available(), SETUP_FILE_TXT);
    }

    @Test
    public void When_DownloadingFileFromStorage_Expect_FileContentToMatch() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(storageConnector.downloadFromStorage(SETUP_FILE_TXT).readAllBytes());
        downloadedFile = new File(FILE_EXPECTED_DOWNLOAD);
        FileUtils.copyToFile(inputStream, downloadedFile);

        assertThat(FileUtils.contentEquals(uploadedFile, downloadedFile), is(true));
    }

    @AfterEach
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void deleteDownloadedFile() {
        downloadedFile.delete();
    }
}
