package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LinkSetMapperParameters {
    private boolean isNested;
    private String linkSetId;
    private String conditionNamed;
    private String qualifier;
    private String clinicalStatusCode;
    private String effectiveTimeHigh;
    private String effectiveTimeLow;
    private String availabilityTime;
    private String relatedClinicalContent;
}