package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;
import java.util.List;

import org.joda.time.DateTime;

import lombok.Builder;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;

@Builder
@Data
public class EhrRequestStatus {

    private DateTime actionTimestamp;
    private String nhsNumber;
    private String conversationId;

}

