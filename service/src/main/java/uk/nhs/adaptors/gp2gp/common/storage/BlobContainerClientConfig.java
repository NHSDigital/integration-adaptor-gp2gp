package uk.nhs.adaptors.gp2gp.common.storage;

import static uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorOptions.AZURE;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Configuration
public class BlobContainerClientConfig {
    @Autowired
    private StorageConnectorConfiguration configuration;

    @Bean
    public BlobContainerClient getBlobContainerClient() {
        String connectionString = configuration.getAzureConnectionString();
        String containerName = configuration.getContainerName();

        if (configuration.getType().equals(AZURE.getStringValue()) && StringUtils.isNotBlank(connectionString)) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();

            if (blobServiceClient.getBlobContainerClient(configuration.getContainerName()).exists()) {
                return blobServiceClient.getBlobContainerClient(containerName);
            } else {
                return blobServiceClient.createBlobContainer(containerName);
            }
        }
        return null;
    }
}
