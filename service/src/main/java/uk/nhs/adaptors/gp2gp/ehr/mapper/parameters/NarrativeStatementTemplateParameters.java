package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@Builder
public class NarrativeStatementTemplateParameters {
    private String narrativeStatementId;
    private String availabilityTime;
    private String comment;
    private boolean isNested;
}
