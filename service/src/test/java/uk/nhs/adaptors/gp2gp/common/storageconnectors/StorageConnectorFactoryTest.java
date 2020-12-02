package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.AZURE;
import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.S3;

@ContextConfiguration
@SpringBootTest
public class StorageConnectorFactoryTest {

    @Autowired
    private StorageConnector storageConnector;
    @Autowired
    private StorageConnectorConfiguration configuration;

    @Test
    public void checkFactoryReturnsCorrectConnector() {
        switch (configuration.getPlatform()) {
            case S3:
                assertTrue(storageConnector instanceof S3StorageConnector);
                break;
            case AZURE:
                assertTrue(storageConnector instanceof AzureStorageConnector);
                break;
            default:
                assertTrue(storageConnector instanceof LocalMockConnector);
        }
    }
}
