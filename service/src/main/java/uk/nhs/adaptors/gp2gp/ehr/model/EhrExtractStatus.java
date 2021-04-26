package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.nhs.adaptors.gp2gp.common.mongo.ttl.TimeToLive;

import java.time.Instant;
import java.util.List;

@CompoundIndexes({
        @CompoundIndex(
                name = EhrExtractStatus.EHR_EXTRACT_STATUS_UNIQUE_INDEX,
                def = "{'conversationId': 1}",
                unique = true)
})
@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EhrExtractStatus implements TimeToLive {
    public static final String EHR_EXTRACT_STATUS_UNIQUE_INDEX = "ehr_extract_status_unique_index";

    @Id
    private String id;
    private Instant created;
    private Instant updatedAt;
    private String conversationId;
    private EhrRequest ehrRequest;
    private GpcAccessStructured gpcAccessStructured;
    private GpcAccessDocument gpcAccessDocument;
    private EhrExtractCore ehrExtractCore;
    private EhrContinue ehrContinue;
    private EhrReceivedAcknowledgement ehrReceivedAcknowledgement;

    public EhrExtractStatus(Instant created, Instant updatedAt, String conversationId, EhrRequest ehrRequest) {
        this.created = created;
        this.updatedAt = updatedAt;
        this.conversationId = conversationId;
        this.ehrRequest = ehrRequest;
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
        private String messageId;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcAccessStructured {
        private String objectName;
        private Instant accessedAt;
        private String taskId;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcAccessDocument {
        private List<GpcDocument> documents;
        private String patientId;

        @Data
        @AllArgsConstructor
        @Document
        @Builder
        public static class GpcDocument {
            private String documentId;
            private String accessDocumentUrl;
            private String objectName;
            private Instant accessedAt;
            private String taskId;
            private String messageId;
        }
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class EhrExtractCore {
        private Instant sentAt;
        private String taskId;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class EhrContinue {
        private Instant received;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class EhrReceivedAcknowledgement {
        private String rootId;
        private Instant received;
        private Instant conversationClosed;
        private List<ErrorDetails> errors;
        private String messageRef;

        @Data
        @AllArgsConstructor
        @Document
        @Builder
        public static class ErrorDetails {
            private String code;
            private String display;
        }
    }
}
