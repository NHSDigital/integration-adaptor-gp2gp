package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EhrStatusRequest {

    private Instant initialRequestTimestamp;
    private Instant actionCompletedTimestamp;
    private String nhsNumber;
    private String conversationId;
    private MigrationStatus migrationStatus;
    private String fromAsid;
    private String toAsid;
    private String fromOdsCode;
    private String toOdsCode;

}

