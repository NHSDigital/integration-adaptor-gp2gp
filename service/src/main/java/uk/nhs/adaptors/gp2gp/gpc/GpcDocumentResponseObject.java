package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GpcDocumentResponseObject {
    private String type;
    private String conversationId;
    private String taskId;
    private String response;
}
