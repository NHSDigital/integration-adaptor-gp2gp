package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface StorageConnector {
    void uploadToStorage(InputStream is, String filename) throws IOException;
    OutputStream downloadFromStorage(String filename) throws IOException;
    List<String> getFileListFromStorage();
}
