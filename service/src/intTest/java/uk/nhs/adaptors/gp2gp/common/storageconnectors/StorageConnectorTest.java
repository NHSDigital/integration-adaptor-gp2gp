package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.annotation.AfterTestClass;

@SpringBootTest
public class StorageConnectorTest {

    @Autowired
    private StorageConnector storageConnector;

    private String fileToUpload = "src/intTest/resources/test.txt";
    private String fileExpectedDownload = "src/intTest/resources/downloads/test.txt";
    private static final String setupFile = "setupFile.txt";
    private File upload;
    private File download;

    @BeforeEach
    public void setupConnector() throws IOException {
        InputStream inputStream = FileUtils.openInputStream(upload);
        storageConnector.uploadToStorage(inputStream, inputStream.available(), setupFile);
    }

    @Test
    public void When_DownloadingFileFromStorage_ExpectFileContentToBeCorrect() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(storageConnector.downloadFromStorage(setupFile).readAllBytes());

        FileUtils.copyToFile(inputStream, download);
        assertEquals(FileUtils.readLines(upload), FileUtils.readLines(download));
    }

    @AfterTestClass
    public void deleteDownloadedFile() {
        new File(fileExpectedDownload).delete();
    }

    @BeforeEach
    private void initialiseTestVariables() {
        fileToUpload = getClass().getResource("/test.txt").getPath();
        fileExpectedDownload = getClass().getResource("/downloads/test.txt").getPath();
        System.out.println(fileToUpload);
        upload = new File(fileToUpload);
        download = new File(fileExpectedDownload);
    }
}
