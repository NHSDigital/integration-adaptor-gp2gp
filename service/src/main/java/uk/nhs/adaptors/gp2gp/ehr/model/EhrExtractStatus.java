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
        /**
         * UUID for tracking the journey of the request
         */
        private String requestId;
        /**
         * NHS Number for patient being migrated
         */
        private String nhsNumber;
        /**
         * todo: find out what these are
         */
        private String fromPartyId;
        /**
         * todo: find out what these are
         */
        private String toPartyId;
        /**
         * todo: find out what these are
         */
        private String fromAsid;
        /**
         * todo: find out what these are
         */
        private String toAsid;
        /**
         * todo: find out what these are
         */
        private String fromOdsCode;
        /**
         * todo: find out what these are
         */
        private String toOdsCode;
        /**
         * todo: find out what these are
         */
        private String messageId;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcDocument {
        /**
         * Unique identifier of the GpcDocument in GPC Document storage. UUIDv4 format
         */
        private String documentId;
        /**
         * URL of the document resource to retrieve from GPC
         */
        private String accessDocumentUrl;
        /**
         * Name of the document as stored in configured storage todo:// check this is correct
         */
        private String objectName;
        /**
         * Time that the document was retrieved
         */
        private Instant accessedAt;
        /**
         * The id of the task created to retrieve the document
         */
        private String taskId;
        /**
         * The ID of the message this is in response todo:// check this is right
         */
        private String messageId;
        /**
         * Details of when respective documents were sent to MHS Outbound
         */
        private GpcAccessDocument.SentToMhs sentToMhs;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcAccessStructured {
        /**
         * Name of the Structured Record as stored in configured storage todo:// check this is correct
         */
        private String objectName;
        /**
         * Time that the Structured Record was retrieved
         */
        private Instant accessedAt;
        /**
         * The ID of the task created to retrieve the Structured Record
         */
        private String taskId;
        /**
         * The ID of the GpcDocument that is attached to the record
         */
        private GpcDocument attachment;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class GpcAccessDocument {
        /**
         * List of documents that are linked to the request
         */
        private List<GpcDocument> documents;
        /**
         * TODO:// check this
         */
        private String patientId;

        @Data
        @AllArgsConstructor
        @Document
        @Builder
        public static class SentToMhs {
            /**
             * The list of message ids for the outbound messages
             */
            private List<String> messageId;
            /**
             * Time that messages were sent to MHS Outbound
             */
            private String sentAt;
            /**
             * The ID of the task created to send the documents to MHS
             */
            private String taskId;
        }
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class EhrExtractCore {
        /**
         * The time the EhrExtractCore was sent
         */
        private Instant sentAt;
        /**
         * The ID of the task created to send the EhrExtractCore
         */
        private String taskId;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class EhrExtractCorePending {
        /**
         * The time the EhrExtractCore was pending being sent
         */
        private Instant sentAt;
        /**
         * The ID of the task created to send the EhrExtractCore
         */
        private String taskId;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class EhrContinue {
        /**
         * The time that the "continue" message was received from requesting system
         */
        private Instant received;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class AckToRequester {
        /**
         * The ID of the task created to send an ACK back to the requesting system
         */
        private String taskId;
        /**
         * The ID of the task created to send the EhrExtractCore
         */
        private String messageId;
        /**
         * The type of acknowledgement being sent
         */
        private String typeCode;
        /**
         * The reason for the acknowledgement
         */
        private String reasonCode;
        /**
         * Details of any errors that may have occurred
         */
        private String detail;
    }

    @Data
    @AllArgsConstructor
    @Document
    @Builder
    public static class AckPending {
        /**
         * The ID of the task created to send an ACK back to the requesting system
         */
        private String taskId;
        /**
         * The ID of the message this is in response todo:// check this is right
         */
        private String messageId;
        /**
         * Type code for the acknowledgement being sent to the Requesting System
         */
        private String typeCode;
        /**
         * Time that ack the acknowledgement was pending to be sent
         */
        private String updatedAt;
    }

    // TODO: 24/09/2021 Check purpose of these objects 
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
