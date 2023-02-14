package uk.nhs.adaptors.gp2gp.ehr.status.model;

import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class EhrStatusRequestQuery {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime fromDateTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime toDateTime;
    private String fromAsid;
    private String toAsid;
    private String fromOdsCode;
    private String toOdsCode;

}

