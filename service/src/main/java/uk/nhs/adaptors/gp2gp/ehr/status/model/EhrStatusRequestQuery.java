package uk.nhs.adaptors.gp2gp.ehr.status.model;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhrStatusRequestQuery {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant fromDateTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant toDateTime;
    private String fromAsid;
    private String toAsid;
    private String fromOdsCode;
    private String toOdsCode;

}

