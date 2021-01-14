package uk.nhs.adaptors.gp2gp.ehr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import uk.nhs.adaptors.gp2gp.common.mongo.ttl.TimeToLive;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@CompoundIndexes({
    @CompoundIndex(
        name = EhrExtractStatus.EHR_EXTRACT_STATUS_UNIQUE_INDEX,
        def = "{'conversationId': 1}",
        unique = true)
})
@Data
@AllArgsConstructor
@Document
@Builder
public class EhrExtractStatus implements TimeToLive {
    public static final String EHR_EXTRACT_STATUS_UNIQUE_INDEX = "ehr_extract_status_unique_index";

    @Id
    private String id;

    private Instant created;
    private Instant updatedAt;
    private String conversationId;
    private EhrRequest ehrRequest;
    private GpcStructured gpcStructured;
    private List<GpcDocument> gpcDocument;

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
    @Builder
    public static class GpcStructured {
        private String value;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcDocument {
        private String value;
    }
}
