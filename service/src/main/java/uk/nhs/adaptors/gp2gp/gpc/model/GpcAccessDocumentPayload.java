package uk.nhs.adaptors.gp2gp.gpc.model;

import lombok.Data;

@Data
public class GpcAccessDocumentPayload {
    private String resourceType;
    private String id;
    private String contentType;
    private String content;
}
