package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.AZURE;
import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.S3;
import lombok.Setter;

import org.springframework.beans.factory.FactoryBean;

@Setter
public class StorageConnectorFactory implements FactoryBean<StorageConnector> {

    private static StorageConnector storageConnector;
    private StorageConnectorConfiguration configuration;

    @Override
    public StorageConnector getObject() throws Exception {
        if (storageConnector == null) {
            switch (configuration.getPlatform()) {
                case S3:
                    storageConnector = new S3StorageConnector(configuration);
                    break;
                case AZURE:
                    storageConnector = new AzureStorageConnector(configuration);
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

    @Override
    public boolean isSingleton() {
        return true;
    }
}
