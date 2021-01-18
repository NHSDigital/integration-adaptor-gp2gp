package uk.nhs.adaptors.gp2gp.common.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class StorageConnectorService {
    private final StorageConnector storageConnector;
    private final ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public void uploadWithMetadata(StorageDataWrapper response) {
        String jsonStringResponse = objectMapper.writeValueAsString(response);
        var responseBytes = jsonStringResponse.getBytes(UTF_8);
        var responseInputStream = new ByteArrayInputStream(responseBytes);
        storageConnector.uploadToStorage(responseInputStream, responseBytes.length,
            response.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
    }
}
