package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NarrativeStatementTemplateParameters {
    private String narrativeStatementId;
    private String availabilityTime;
    private String comment;
    private boolean isNested;
}
