package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalMockConnector implements StorageConnector {
    private final Map<String, byte[]> storage;

    protected LocalMockConnector() {
        storage = new ConcurrentHashMap<>();
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        try {
            storage.put(filename, is.readAllBytes());
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred uploading to Mock Storage", ioException);
        }
    }

    @Override
    public InputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            byte[] objectBytes = storage.get(filename);
            return new ByteArrayInputStream(objectBytes);
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred downloading from Mock Storage", exception);
        }
    }
}
