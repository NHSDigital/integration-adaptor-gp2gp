package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class StorageConnectorFactoryTest {

    @Test
    public void checkFactoryReturnsMockConnector() {
        StorageConnector storageConnector = StorageConnectorFactory.getConfiguredConnector();
        assertTrue(storageConnector instanceof LocalMockConnector);
    }

}
