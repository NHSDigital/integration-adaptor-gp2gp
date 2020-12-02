package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.InputStream;
import java.io.OutputStream;

public interface StorageConnector {
    void uploadToStorage(InputStream is, String filename) throws StorageConnectorException;
    OutputStream downloadFromStorage(String filename) throws StorageConnectorException;
}
