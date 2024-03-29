package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;

@Builder
@Data
public class EhrStatus {

    private List<AttachmentStatus> attachmentStatus;
    private List<EhrExtractStatus.EhrReceivedAcknowledgement> migrationLog;
    private MigrationStatus migrationStatus;
    private Instant originalRequestDate;
    private String fromAsid;
    private String toAsid;

    @Builder
    @Data
    public static class AttachmentStatus {
        private List<Identifier> identifier;
        private FileStatus fileStatus;
        private String fileName;
        private String originalDescription;
    }
}

