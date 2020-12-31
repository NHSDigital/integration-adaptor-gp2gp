package uk.nhs.adaptors.gp2gp.ehr;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@CompoundIndexes({
    @CompoundIndex(
        name = "ehr_extract_status_unique_index",
        def = "{'conversationId': 1}",
        unique = true)
})
@Data
@AllArgsConstructor
@Document
public class EhrExtractStatus {
    private Instant created;
    private Instant updatedAt;
    private String conversationId;
    private EhrRequest ehrRequest;

    @Data
    @AllArgsConstructor
    @Document
    public static class EhrRequest {
        private String requestId;
        private String nhsNumber;
        private String fromPartyId;
        private String toPartyId;
        private String fromAsid;
        private String toAsid;
        private String fromOdsCode;
        private String toOdsCode;
    }
}
