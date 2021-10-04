package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Collection;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.springframework.stereotype.Component;

@Component
public class BloodPressureValidator {
    static final String BLOOD_PRESSURE_READING_CODE = "163020007";
    static final String ARTERIAL_BLOOD_PRESSURE_CODE = "386534000";
    static final String BLOOD_PRESSURE_CODE = "75367002";
    static final String STANDING_BLOOD_PRESSURE_CODE = "163034007";
    static final String SITTING_BLOOD_PRESSURE_CODE = "163035008";
    static final String LYING_BLOOD_PRESSURE_CODE = "163033001";

    static final String SYSTOLIC_ARTERIAL_PRESSURE = "72313002";
    static final String SYSTOLIC_BLOOD_PRESSURE = "271649006";
    static final String SYSTOLIC_STANDING_BLOOD_PRESSURE = "400974009";
    static final String SYSTOLIC_SITTING_BLOOD_PRESSURE = "407554009";
    static final String SYSTOLIC_LYING_BLOOD_PRESSURE = "407556006";

    static final String DIASTOLIC_ARTERIAL_PRESSURE = "1091811000000102";
    static final String DIASTOLIC_BLOOD_PRESSURE = "271650006";
    static final String DIASTOLIC_STANDING_BLOOD_PRESSURE = "400975005";
    static final String DIASTOLIC_SITTING_BLOOD_PRESSURE = "407555005";
    static final String DIASTOLIC_LYING_BLOOD_PRESSURE = "407557002";

    public boolean isValidBloodPressure(Observation observation) {
        if (CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(
            BLOOD_PRESSURE_READING_CODE, ARTERIAL_BLOOD_PRESSURE_CODE, BLOOD_PRESSURE_CODE
        ))) {
            if (hasBloodPressureCode(observation, List.of(SYSTOLIC_ARTERIAL_PRESSURE, SYSTOLIC_BLOOD_PRESSURE))) {
                return hasBloodPressureCode(observation, List.of(DIASTOLIC_ARTERIAL_PRESSURE, DIASTOLIC_BLOOD_PRESSURE));
            }
            return false;
        } else {
            return hasTriple(observation, STANDING_BLOOD_PRESSURE_CODE, SYSTOLIC_STANDING_BLOOD_PRESSURE, DIASTOLIC_STANDING_BLOOD_PRESSURE)
                || hasTriple(observation, SITTING_BLOOD_PRESSURE_CODE, SYSTOLIC_SITTING_BLOOD_PRESSURE, DIASTOLIC_SITTING_BLOOD_PRESSURE)
                || hasTriple(observation, LYING_BLOOD_PRESSURE_CODE, SYSTOLIC_LYING_BLOOD_PRESSURE, DIASTOLIC_LYING_BLOOD_PRESSURE);
        }
    }

    private static boolean hasTriple(Observation observation, String panelCode, String systolicBloodPressureCode,
        String diastolicBloodPressureCode) {
        return CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(panelCode))
            && hasBloodPressureCode(observation, List.of(systolicBloodPressureCode))
            && hasBloodPressureCode(observation, List.of(diastolicBloodPressureCode));
    }

    private static boolean hasBloodPressureCode(Observation observation, Collection<String> codes) {
        return observation.getComponent()
            .stream()
            .flatMap(observationComponentComponent -> observationComponentComponent.getCode().getCoding().stream())
            .map(Coding::getCode)
            .anyMatch(codes::contains);
    }
}
