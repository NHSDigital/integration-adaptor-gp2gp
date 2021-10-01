package uk.nhs.adaptors.gp2gp.ehr.utils;

import com.google.common.collect.Sets;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator.*;

class BloodPressureValidatorTest {
    private final BloodPressureValidator bloodPressureValidator = new BloodPressureValidator();

    private static Stream<Arguments> validCodeTriples() {
        return Stream.concat(
            Sets.cartesianProduct(
                Set.of(BLOOD_PRESSURE_READING_CODE, ARTERIAL_BLOOD_PRESSURE_CODE, BLOOD_PRESSURE_CODE),
                Set.of(SYSTOLIC_ARTERIAL_PRESSURE, SYSTOLIC_BLOOD_PRESSURE),
                Set.of(DIASTOLIC_ARTERIAL_PRESSURE, DIASTOLIC_BLOOD_PRESSURE)
            ).stream().map(args -> Arguments.of(args.toArray())),
            Stream.of(
                Arguments.of(STANDING_BLOOD_PRESSURE_CODE, SYSTOLIC_STANDING_BLOOD_PRESSURE, DIASTOLIC_STANDING_BLOOD_PRESSURE),
                Arguments.of(SITTING_BLOOD_PRESSURE_CODE, SYSTOLIC_SITTING_BLOOD_PRESSURE, DIASTOLIC_SITTING_BLOOD_PRESSURE),
                Arguments.of(LYING_BLOOD_PRESSURE_CODE, SYSTOLIC_LYING_BLOOD_PRESSURE, DIASTOLIC_LYING_BLOOD_PRESSURE)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("validCodeTriples")
    void Given_ValidCodeTriple_Expect_ValidBloodPressure(String panelCode, String systolicBloodPressureCode, String diastolicBloodPressureCode) {
        // given
        Observation observation = createObservation(panelCode, systolicBloodPressureCode, diastolicBloodPressureCode);
        
        // expect
        assertThat(bloodPressureValidator.isValidBloodPressure(observation)).isTrue();
    }

    // TODO: this should be implemented with some kind of property testing library
    @Test
    void Given_InvalidCodeTriple_Expect_InvalidBloodPressure() {
        // given
        Observation observation = createObservation("invalidPanelCode", "invalidSystolicBloodPressureCode", "invalidDiastolicBloodPressureCode");

        // expect
        assertThat(bloodPressureValidator.isValidBloodPressure(observation)).isFalse();
    }
    
    private static Observation createObservation(String panelCode, String systolicBloodPressureCode, String diastolicBloodPressureCode) {
        return new Observation()
            .setCode(createCodeConcept(panelCode))
            .addComponent(new Observation.ObservationComponentComponent().setCode(createCodeConcept(systolicBloodPressureCode)))
            .addComponent(new Observation.ObservationComponentComponent().setCode(createCodeConcept(diastolicBloodPressureCode)));
    }
    
    private static CodeableConcept createCodeConcept(String code) {
        return new CodeableConcept().addCoding(new Coding().setCode(code));
    }
}