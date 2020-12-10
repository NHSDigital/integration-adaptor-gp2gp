package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.InputStream;

public interface StorageConnector {
    void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException;
    InputStream downloadFromStorage(String filename) throws StorageConnectorException;
}
