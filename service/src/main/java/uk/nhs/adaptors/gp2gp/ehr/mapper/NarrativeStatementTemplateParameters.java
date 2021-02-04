package uk.nhs.adaptors.gp2gp.ehr.mapper;

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
}
