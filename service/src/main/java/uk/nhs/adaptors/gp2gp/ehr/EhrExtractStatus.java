package uk.nhs.adaptors.gp2gp.ehr;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.mongo.ttl.TimeToLive;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@CompoundIndexes({
    @CompoundIndex(
        name = EhrExtractStatus.EHR_EXTRACT_STATUS_UNIQUE_INDEX,
        def = "{'conversationId': 1}",
        unique = true)
})
@Data
@Document
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EhrExtractStatus implements TimeToLive {
    public static final String EHR_EXTRACT_STATUS_UNIQUE_INDEX = "ehr_extract_status_unique_index";

    private Instant created;
    private Instant updatedAt;
    @Id
    private String id;
    private String conversationId;
    private EhrRequest ehrRequest;
    private List<GpcAccessDocument> gpcAccessDocuments;

    public EhrExtractStatus(Instant created, Instant updatedAt, String conversationId, EhrRequest ehrRequest, List<GpcAccessDocument> gpcAccessDocuments) {
        this.created = created;
        this.updatedAt = updatedAt;
        this.conversationId = conversationId;
        this.ehrRequest = ehrRequest;
        this.gpcAccessDocuments = gpcAccessDocuments;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
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

    @Data
    @AllArgsConstructor
    @Document
    public static class GpcAccessDocument {
        private String objectName;
        private Instant accessedAt;
        private String taskId;
    }
}
