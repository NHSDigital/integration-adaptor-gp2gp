package uk.nhs.adaptors.gp2gp.common.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.task.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.gpc.GpcConfiguration;
import uk.nhs.adaptors.gp2gp.gpc.GpcStructuredResponseObject;

@Service
@AllArgsConstructor
public class StorageConnectorService {
    private final StorageConnectorFactory storageConnectorFactory;
    private final ObjectMapper objectMapper;
    private final GpcConfiguration gpcConfiguration;
    private final StorageUtils storageUtils;

    public void handleStructuredRecord(GpcStructuredResponseObject response) throws IOException {
        String jsonStringResponse = objectMapper.writeValueAsString(response);
        var responseInputStream = new ByteArrayInputStream(jsonStringResponse.getBytes(UTF_8));

        var storageConnector = Optional.ofNullable(storageConnectorFactory.getObject())
            .orElseThrow(() ->
                new TaskHandlerException("No storage connector available"));

        var length = storageUtils.getInputStreamSize(responseInputStream);
        storageConnector.uploadToStorage(responseInputStream, length, response.getConversationId() + "_gpc_structured.json");
    }
}
