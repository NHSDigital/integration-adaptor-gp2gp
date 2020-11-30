package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;

public class AzureStorageConnector implements StorageConnector {

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    private static final String CONTAINER_NAME = "for-nia-testing";

    protected AzureStorageConnector() {
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        if (connectionString != null && !connectionString.isEmpty()) {
            blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();

            if (blobServiceClient.getBlobContainerClient(CONTAINER_NAME).exists()) {
                containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
            } else {
                containerClient = blobServiceClient.createBlobContainer(CONTAINER_NAME);
            }
        }
    }

    @Override
    public void uploadToStorage(InputStream is, String filename) throws IOException {
        BlobClient blobClient = containerClient.getBlobClient(filename);
        blobClient.upload(is, is.available());
    }

    @Override
    public OutputStream downloadFromStorage(String filename) throws IOException {
        BlobClient blobClient = containerClient.getBlobClient(filename);
        OutputStream outputStream = new ByteArrayOutputStream();
        blobClient.openInputStream().transferTo(outputStream);
        return outputStream;
    }

    @Override
    public List<String> getFileListFromStorage() {
        return containerClient.listBlobs().stream()
            .map(BlobItem::getName)
            .collect(Collectors.toList());
    }

}
