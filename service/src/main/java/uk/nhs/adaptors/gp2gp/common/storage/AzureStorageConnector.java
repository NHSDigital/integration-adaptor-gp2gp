package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.InputStream;

import uk.nhs.adaptors.gp2gp.exceptions.StorageConnectorException;

import org.springframework.beans.factory.annotation.Autowired;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

public class AzureStorageConnector implements StorageConnector {
    @Autowired
    private BlobContainerClient containerClient;

    protected AzureStorageConnector() {
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            blobClient.upload(is, streamLength);
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred uploading to Azure Storage", exception);
        }
    }

    @Override
    public InputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            return blobClient.openInputStream();
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred downloading from Azure Storage", exception);
        }
    }
}
