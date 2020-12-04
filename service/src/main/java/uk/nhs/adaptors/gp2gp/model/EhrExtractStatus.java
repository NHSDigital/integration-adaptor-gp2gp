package uk.nhs.adaptors.gp2gp.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@CompoundIndexes({
    @CompoundIndex(
        name = "ehr_extract_status",
        def = "{'extractId': 1, 'created': 1}",
        unique = true)
})
@Data
@AllArgsConstructor
@Document
public class EhrExtractStatus {
    @Id
    private String extractId;
    private LocalDateTime created;
}
