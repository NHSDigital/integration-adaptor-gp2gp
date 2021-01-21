package uk.nhs.adaptors.gp2gp.ehr;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {
    private String id;
    private Instant time;
    private String signatureCode;
    private String signatureText;
    private String organisationId;
}
