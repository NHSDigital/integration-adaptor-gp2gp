package uk.nhs.adaptors.gp2gp.gpc;

import lombok.Data;

@Data
public class GpcPatientDocument {
    private String resourceType;
    private String id;
    private String contentType;
    private String content;
}
