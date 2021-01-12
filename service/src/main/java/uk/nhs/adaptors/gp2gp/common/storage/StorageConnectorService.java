package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Service
@AllArgsConstructor
public class StorageConnectorService {
    private final StorageConnectorFactory storageConnectorFactory;

    public void handleStructuredRecord(ByteArrayInputStream response, GetGpcStructuredTaskDefinition structuredTaskDefinition) throws IOException {
        var storageConnector = storageConnectorFactory.getObject();
        int length = response.available();
        //need content length from respone header, currently not provided.. can we request this to be added?

        String fileName = structuredTaskDefinition.getConversationId()+ "_gpc_structured.json";
        storageConnector.uploadToStorage(response, length, fileName);

        //temp debug to confirm object is stored correctly...
        String encodedString = Base64.getEncoder().encodeToString(storageConnector.downloadFromStorage(fileName).readAllBytes());
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String decodedString = new String(decodedBytes);
    }
}
