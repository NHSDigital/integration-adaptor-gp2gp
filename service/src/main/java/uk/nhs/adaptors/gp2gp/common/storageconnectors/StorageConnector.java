package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.InputStream;

import uk.nhs.adaptors.gp2gp.exceptions.StorageConnectorException;

public interface StorageConnector {
    void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException;
    InputStream downloadFromStorage(String filename) throws StorageConnectorException;
}
