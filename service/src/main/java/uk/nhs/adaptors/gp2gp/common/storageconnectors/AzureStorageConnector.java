package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

public class AzureStorageConnector implements StorageConnector {

    @Autowired
    private BlobContainerClient containerClient;

    protected AzureStorageConnector() {}

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
