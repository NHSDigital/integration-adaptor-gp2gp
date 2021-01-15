package uk.nhs.adaptors.gp2gp.common.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StorageDataWrapper {
    private String type;
    private String conversationId;
    private String taskId;
    private String response;
}
