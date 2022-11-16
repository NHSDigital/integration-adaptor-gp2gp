package uk.nhs.adaptors.gp2gp.e2e.model;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class EhrStatus {
    private List<AttachmentStatus> attachmentStatus;
    private List<ReceivedAck> migrationLog;
    private MigrationStatus migrationStatus;
    private Instant originalRequestDate;
    private String fromAsid;
    private String toAsid;

    @Data
    public static class ReceivedAck {
        private String rootId;
        private Instant received;
        private Instant conversationClosed;
        private List<ErrorDetails> errors;
        private String messageRef;

        @Data
        public static class ErrorDetails {
            private String code;
            private String display;
        }
    }

    public enum MigrationStatus {
        COMPLETE,
        COMPLETE_WITH_ISSUES,
        FAILED_NME,
        FAILED_INCUMBENT,
        IN_PROGRESS
    }

    @Data
    public static class AttachmentStatus {
        private List<Identifier> identifier;
        private FileStatus fileStatus;
        private String fileName;
        private String originalDescription;

        @Data
        public static class Identifier {
            private String system;
            private String value;
        }

        public enum FileStatus {
            PLACEHOLDER,
            ORIGINAL_FILE,
            SKELETON_MESSAGE,
            ERROR
        }
    }
}

