package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@Builder
@Data
public class EhrStatus {

    private List<AttachmentStatus> attachmentStatus;
    private List<EhrExtractStatus.EhrReceivedAcknowledgement> acknowledgementModel;
    private MigrationStatus migrationStatus;
    private Instant originalRequestDate;

    @Builder
    @Data
    public static class AttachmentStatus {
        // TODO: add identifier
        private String url;
        private String title;
        private FileStatus fileStatus;
    }
}

