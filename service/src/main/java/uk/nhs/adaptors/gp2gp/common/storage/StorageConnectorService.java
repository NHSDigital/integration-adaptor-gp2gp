package uk.nhs.adaptors.gp2gp.common.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileInputStream;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class StorageConnectorService {
    private final StorageConnector storageConnector;
    private final ObjectMapper objectMapper;

    @SneakyThrows({JsonProcessingException.class, IOException.class})
    public void uploadFile(StorageDataWrapper response, String filename) {
        File tempFile = File.createTempFile("StorageConnectorService-uploadFile-", ".json");
        
        try {
            objectMapper.writeValue(tempFile, response);
            try (var responseInputStream = new FileInputStream(tempFile)) {
                storageConnector.uploadToStorage(responseInputStream, tempFile.length(), filename);
            }
        } finally{
            tempFile.delete();
        }
    }

    @SneakyThrows
    public StorageDataWrapper downloadFile(String filename) {
        String stringDownload;
        try (var inputStream = storageConnector.downloadFromStorage(filename)) {
            stringDownload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
        return objectMapper.readValue(stringDownload, StorageDataWrapper.class);
    }
}
