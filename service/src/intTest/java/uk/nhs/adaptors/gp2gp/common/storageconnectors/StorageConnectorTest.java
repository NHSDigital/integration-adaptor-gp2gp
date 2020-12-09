package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import uk.nhs.adaptors.gp2gp.extension.IntegrationTestsExtension;

import org.apache.commons.io.FileUtils;
import org.junit.After;
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

    private static final String SETUP_FILE_TXT = "setupFile.txt";
    private byte[] uploadedFileBytes;
    private InputStream downloadedFileStream;

    @BeforeEach
    public void setupConnector() throws IOException {
        String fileToUpload = StorageConnectorTest.class.getResource("/test.txt").getPath();
        File uploadedFile = new File(fileToUpload);

        InputStream tempUploadedFileStream = FileUtils.openInputStream(uploadedFile);
        uploadedFileBytes = tempUploadedFileStream.readAllBytes();
        tempUploadedFileStream.close();

        InputStream uploadedFileStream = FileUtils.openInputStream(uploadedFile);
        storageConnector.uploadToStorage(uploadedFileStream, uploadedFileStream.available(), SETUP_FILE_TXT);
        uploadedFileStream.close();
    }

    @Test
    public void When_DownloadingFileFromStorage_Expect_FileContentToMatch() throws IOException {
        downloadedFileStream = storageConnector.downloadFromStorage(SETUP_FILE_TXT);

        assertThat(downloadedFileStream.readAllBytes(), is(uploadedFileBytes));
    }

    @After
    public void tearDown() throws IOException {
        downloadedFileStream.close();
    }
}
