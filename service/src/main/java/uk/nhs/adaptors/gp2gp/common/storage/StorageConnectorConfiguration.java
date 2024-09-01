package uk.nhs.adaptors.gp2gp.common.storage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.s3.S3Client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gp2gp.storage")
public class StorageConnectorConfiguration {
    private static final String S3_PREFIX = "s3";

    private String type;
    private String containerName;
    private String azureConnectionString;
    private String trustStoreUrl;
    private String trustStorePassword;

    @Bean
    public S3Client getS3Client() {
        if (StringUtils.isNotBlank(trustStoreUrl) && trustStoreUrl.startsWith(S3_PREFIX)) {
            return S3Client.builder().build();
        }

        return null;
    }
}
