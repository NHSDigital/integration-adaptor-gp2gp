package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class LocalMockConnector implements StorageConnector {

    private final Map<String, byte[]> storage;

    protected LocalMockConnector() {
        storage = new HashMap<>();
    }

    @Override
    public void uploadToStorage(InputStream is, String filename) throws StorageConnectorException {
        try {
            storage.put(filename, is.readAllBytes());
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred uploading to Mock Storage", ioException);
        }
    }

    @Override
    public OutputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            byte[] objectBytes = storage.get(filename);
            OutputStream returnObject = new ByteArrayOutputStream();
            InputStream is = new ByteArrayInputStream(objectBytes);

            is.transferTo(returnObject);

            return returnObject;
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred downloading from Mock Storage", ioException);
        }
    }
}
