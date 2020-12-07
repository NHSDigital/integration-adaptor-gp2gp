package uk.nhs.adaptors.gp2gp.common.storageconnectors;
import lombok.Setter;

import org.springframework.beans.factory.FactoryBean;

@Setter
public class StorageConnectorFactory implements FactoryBean<StorageConnector> {
    private static StorageConnector storageConnector;

    private StorageConnectorConfiguration configuration;

    @Override
    public StorageConnector getObject() {
        if (storageConnector == null) {
            switch (StorageConnectorOptions.enumOf(configuration.getType())) {
                case S3:
                    storageConnector = new S3StorageConnector(configuration);
                    break;
                case AZURE:
                    storageConnector = new AzureStorageConnector();
                    break;
                default:
                    storageConnector = new LocalMockConnector();
            }
        }
        return storageConnector;
    }

    @Override
    public Class<?> getObjectType() {
        return StorageConnector.class;
    }
}
