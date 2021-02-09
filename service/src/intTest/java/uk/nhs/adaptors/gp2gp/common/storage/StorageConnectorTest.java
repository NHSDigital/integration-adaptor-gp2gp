package uk.nhs.adaptors.gp2gp.common.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@DirtiesContext
public class StorageConnectorTest {
    @Autowired
    private StorageConnector storageConnector;

    @Test
    public void When_FileUploadedToStorage_Expect_CanDownloadSameExactFile() throws IOException {
        var filename = UUID.randomUUID().toString() + ".txt";
        try (var fileUploadStream = StorageConnectorTest.class.getResourceAsStream("/test.txt");
             var expectedFileStream = StorageConnectorTest.class.getResourceAsStream("/test.txt")) {
            var expectedFileBytes = expectedFileStream.readAllBytes();
            storageConnector.uploadToStorage(fileUploadStream, expectedFileBytes.length, filename);
            try (var fileDownloadStream = storageConnector.downloadFromStorage(filename)) {
                assertThat(fileDownloadStream.readAllBytes()).isEqualTo(expectedFileBytes);
            }
        }
    }
}
