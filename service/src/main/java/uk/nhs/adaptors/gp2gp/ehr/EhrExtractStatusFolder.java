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
public class EhrExtractStatusFolder {
    private String id;
    private String statusCode;
    private Instant effectiveTimeLow;
    private Instant effectiveTimeHigh;
    private Instant availabilityTime;
    private Author author;
}
