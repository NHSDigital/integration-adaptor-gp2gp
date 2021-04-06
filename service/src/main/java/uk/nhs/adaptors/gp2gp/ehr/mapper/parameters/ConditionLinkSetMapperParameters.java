package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

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
    private String qualifierCode;
    private String qualifierDisplay;
    private String qualifierSignificance;
    private String clinicalStatusCode;
    private String clinicalStatusDisplay;
    private String effectiveTimeHigh;
    private String effectiveTimeLow;
    private String availabilityTime;
    private List<String> relatedClinicalContent;
    private boolean generateObservationStatement;
    private String pertinentInfo;
    private String code;
    private String participant;
}