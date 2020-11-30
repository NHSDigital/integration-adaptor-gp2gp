package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.AZURE_BLOB;
import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.S3;

public class StorageConnectorFactory {

    private static StorageConnector storageConnector;

    public static StorageConnector getConfiguredConnector() {
        String storageMode = System.getenv("STORAGE_SOLUTION");
        if (storageMode == null) {
            storageMode = "LocalMock";
        }
        if (storageConnector == null) {
            switch (storageMode) {
                case S3:
                    storageConnector = new S3StorageConnector();
                    break;
                case AZURE_BLOB:
                    storageConnector = new AzureStorageConnector();
                    break;
                default:
                    storageConnector = new LocalMockConnector();
            }
        }
        return storageConnector;
    }

}
