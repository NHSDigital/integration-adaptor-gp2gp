package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AllergyStructureTemplateParameters {
    private String allergyStructureId;
    private String pertinentInformation;
    private String effectiveTime;
    private String availabilityTime;
}
