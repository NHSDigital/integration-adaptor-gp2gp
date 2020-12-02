package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

public class AzureStorageConnector implements StorageConnector {

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    protected AzureStorageConnector(StorageConnectorConfiguration configuration) {
        String connectionString = configuration.getAzureConnectionString();
        String containerName = configuration.getContainerName();

        if (StringUtils.isNotBlank(connectionString)) {
            blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
            if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
                containerClient = blobServiceClient.getBlobContainerClient(containerName);
            } else {
                containerClient = blobServiceClient.createBlobContainer(containerName);
            }
        }
    }

    @Override
    public void uploadToStorage(InputStream is, String filename) throws StorageConnectorException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            blobClient.upload(is, is.available());
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred uploading to Azure Storage", ioException);
        }
    }

    @Override
    public OutputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            OutputStream outputStream = new ByteArrayOutputStream();
            blobClient.openInputStream().transferTo(outputStream);
            return outputStream;
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred downloading from Azure Storage", ioException);
        }
    }
}
