package uk.nhs.adaptors.gp2gp.ehr;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.common.mongo.ttl.TimeToLive;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@CompoundIndexes({
    @CompoundIndex(
        name = "ehr_extract_status_unique_index",
        def = "{'conversationId': 1, 'requestId': 1}",
        unique = true)
})
@Data
@AllArgsConstructor
@Document
public class EhrExtractStatus implements TimeToLive {
    @Id
    private String extractId;
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
