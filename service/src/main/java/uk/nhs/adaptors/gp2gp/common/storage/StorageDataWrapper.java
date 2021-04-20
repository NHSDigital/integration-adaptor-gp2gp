package uk.nhs.adaptors.gp2gp.common.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorageDataWrapper {
    private String type;
    private String conversationId;
    private String taskId;
    private String data;
}
