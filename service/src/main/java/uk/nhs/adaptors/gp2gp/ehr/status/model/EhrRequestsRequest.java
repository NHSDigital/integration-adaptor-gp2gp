package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;
import java.util.List;

import org.joda.time.DateTime;

import lombok.Builder;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;

@Data
public class EhrRequestsRequest {

    private DateTime fromDateTime;
    private DateTime toDateTime;
    private String practiceASID;

}

