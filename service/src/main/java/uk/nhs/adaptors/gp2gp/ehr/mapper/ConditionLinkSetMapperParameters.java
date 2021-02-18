package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConditionLinkSetMapperParameters {
    private boolean isNested;
    private String linkSetId;
    private String conditionNamed;
    private boolean qualifierIsMajor;
    private String qualifier;
    private boolean clinicalStatusIsActive;
    private String clinicalStatusCode;
    private String effectiveTimeHigh;
    private String effectiveTimeLow;
    private String availabilityTime;
    private List<String> relatedClinicalContent;
    private boolean generateObservationStatement;
    private String pertinentInfo;
}