package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;
import java.util.List;

import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Builder;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;

@Data
public class EhrRequestsRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime fromDateTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime toDateTime;
    private String fromAsid;
    private String toAsid;
    private String fromOdsCode;
    private String toOdsCode;

}

