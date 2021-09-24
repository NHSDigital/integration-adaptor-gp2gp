package uk.nhs.adaptors.gp2gp.ehr.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.mongo.ttl.TimeToLive;

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

    /**
     * Random unique identifier of the EhrExtractStatus. UUIDv4 format
     */
    @Id
    private String id;
    /**
     * Time that the ehrExtractStatus was created / The start time of the EhrExtract Request being processed
     */
    private Instant created;
    /**
     * Timestamp of most recent ehrExtractStatus update
     */
    private Instant updatedAt;
    /**
     * Random unique identifier for tracking progress of the task through
     */
    private String conversationId;
    /**
     * Database Object that tracks information on the received request
     */
    private EhrRequest ehrRequest;
    /**
     * Database Object that tracks the retrieval and storage of the patients structured record
     */
    private GpcAccessStructured gpcAccessStructured;
    /**
     * Database Object that tracks the retrieval and storage of the patients documents
     */
    private GpcAccessDocument gpcAccessDocument;
    /**
     * Database Object that tracks when the ehrExtract is sent to the requesting system
     */
    private EhrExtractCore ehrExtractCore;
    /**
     * Database Object that tracks the readiness of the ehrExtract being ready to send
     */
    private EhrExtractCorePending ehrExtractCorePending;
    /**
     * Database Object that tracks when continue message is received from the requesting system
     */
    private EhrContinue ehrContinue;
    /**
     * Database Object that tracks when the acknowledgement was sent to the requesting system
     */
    private AckToRequester ackToRequester;
    /**
     * Database Object that tracks when we begin waiting for acknowledgement from the requesting system
     */
    private AckPending ackPending;
    /**
     * Database Object that tracks when the acknowledgement was received from the requesting system
     */
    private EhrReceivedAcknowledgement ehrReceivedAcknowledgement;
    /**
     * Database Object that tracks errors at any point in the transformation process
     */
    private Error error;

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
    public static class GpcDocument {
        private String documentId;
        private String accessDocumentUrl;
        private String objectName;
        private Instant accessedAt;
        private String taskId;
        private String messageId;
        private GpcAccessDocument.SentToMhs sentToMhs;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcAccessStructured {
        private String objectName;
        private Instant accessedAt;
        private String taskId;
        private GpcDocument attachment;
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
        public static class SentToMhs {
            private List<String> messageId;
            private String sentAt;
            private String taskId;
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
    public static class EhrExtractCorePending {
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
    public static class AckToRequester {
        private String taskId;
        private String messageId;
        private String typeCode;
        private String reasonCode;
        private String detail;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class AckPending {
        private String taskId;
        private String messageId;
        private String typeCode;
        private String updatedAt;
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

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class Error {
        private Instant occurredAt;
        private String code;
        private String message;
        private String taskType;
    }
}
