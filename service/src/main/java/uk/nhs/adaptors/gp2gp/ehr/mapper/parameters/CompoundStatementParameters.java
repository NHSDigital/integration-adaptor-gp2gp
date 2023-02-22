package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CompoundStatementParameters {
    private boolean nested;
    private String classCode;
    private String id;
    private String compoundStatementCode;
    private String statusCode;
    private String effectiveTime;
    private String availabilityTime;
    private String components;
}
