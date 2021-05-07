package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BloodPressureParameters {
    private String id;
    private String effectiveTime;
    private String availabilityTime;
    private String systolicId;
    private String systolicQuantity;
    private String systolicReferenceRange;
    private String diastolicId;
    private String diastolicQuantity;
    private String diastolicReferenceRange;
    private String narrativeId;
    private String narrativeText;
    private String narrativeAvailabilityTime;
    private boolean isNested;
    private String compoundStatementCode;
    private String systolicCode;
    private String diastolicCode;
    private String participant;
}
