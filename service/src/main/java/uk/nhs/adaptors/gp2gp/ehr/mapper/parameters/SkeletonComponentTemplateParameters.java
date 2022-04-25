package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SkeletonComponentTemplateParameters {
    private String narrativeStatementId;
    private String availabilityTime;
    private String effectiveTime;
}
