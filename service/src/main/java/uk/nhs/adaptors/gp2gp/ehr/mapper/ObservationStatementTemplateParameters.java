package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ObservationStatementTemplateParameters {
    private boolean isNested;
    private String observationStatementId;
    private String availabilityTime;
    private String pertinentInformation;
}
