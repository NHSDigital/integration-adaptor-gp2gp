package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.model.api.Tag;

public class LocalMockConnector implements StorageConnector {
    private final Map<String, byte[]> storage;

    protected LocalMockConnector() {
        storage = new HashMap<>();
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        try {
            List<Tag> tags = new ArrayList<>();
            tags.add(new Tag("Tag 1", "This is tag 1"));
            tags.add(new Tag("Tag 2", "This is tag 2"));

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
