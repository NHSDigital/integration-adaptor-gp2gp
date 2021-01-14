package uk.nhs.adaptors.gp2gp.common.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.gpc.GpcStructuredResponseObject;

@Service
@AllArgsConstructor
public class StorageConnectorService {
    private final StorageConnectorFactory storageConnectorFactory;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
        value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
        justification = "SpotBugs issue with fix not yet released https://github.com/spotbugs/spotbugs/issues/456")
    public void handleStructuredRecord(GpcStructuredResponseObject response) throws IOException {
        String jsonStringResponse = objectMapper.writeValueAsString(response);
        var responseInputStream = new ByteArrayInputStream(jsonStringResponse.getBytes(UTF_8));

        var storageConnector = storageConnectorFactory.getObject();

        var length = jsonStringResponse.getBytes(UTF_8).length;
        storageConnector.uploadToStorage(responseInputStream, length, response.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
    }
}
