package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gp2gp.storage")
public class StorageConnectorConfiguration {
    private String type;
    private String containerName;
    private String azureConnectionString;
}
