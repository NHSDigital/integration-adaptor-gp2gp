package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageConnectorConfiguration {
    private String platform;
    private String containerName;
    private String s3AccessKey;
    private String s3SecretKey;
    private String azureConnectionString;
}
