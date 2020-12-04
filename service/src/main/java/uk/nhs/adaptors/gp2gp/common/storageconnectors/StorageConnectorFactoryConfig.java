package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConnectorFactoryConfig {

    @Autowired
    private StorageConnectorConfiguration configuration;

    @Bean(name = "storage-connector")
    public StorageConnectorFactory storageConnectorFactory() {
        StorageConnectorFactory factory = new StorageConnectorFactory();
        factory.setConfiguration(configuration);
        return factory;
    }

    @Bean
    public StorageConnector storageConnector() {
        return storageConnectorFactory().getObject();
    }
}
