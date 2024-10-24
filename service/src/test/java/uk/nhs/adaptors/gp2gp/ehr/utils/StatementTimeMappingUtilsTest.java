package uk.nhs.adaptors.gp2gp.ehr.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.assertj.core.api.ThrowableAssert;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.MedicationRequest;

import org.hl7.fhir.dstu3.model.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class StatementTimeMappingUtilsTest {

    private static final String MEDICATION_REQUEST_ID = "00000000-0000-4000-8000-000000000001";
    private MedicationRequest medicationRequest;

    @BeforeEach
    void beforeEach() {
        medicationRequest = new MedicationRequest();
        medicationRequest.setId(MEDICATION_REQUEST_ID);
    }

    @Test
    void When_PrepareEffectiveTimeForMedicationRequestWithoutEffectivePeriod_Expect_EhrMapperExceptionContainingIdIsThrown() {
        medicationRequest.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent());

        assertThatEhrMapperExceptionWithIdThrown(
            () -> StatementTimeMappingUtils.prepareEffectiveTimeForMedicationRequest(medicationRequest)
        );
    }

    @Test
    void When_PrepareEffectiveTimeForMedicationRequestWithEffectivePeriodWithoutStart_Expect_EhrMapperExceptionContainingIdIsThrown() {
        medicationRequest.setDispenseRequest(
            new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setValidityPeriod(new Period())
        );

        assertThatEhrMapperExceptionWithIdThrown(
            () -> StatementTimeMappingUtils.prepareEffectiveTimeForMedicationRequest(medicationRequest)
        );
    }

    @Test
    void When_PrepareEffectiveTimeForMedicationRequestWithEffectivePeriodWithStartOnly_Expect_LowXmlElementProduced() {
        medicationRequest.setDispenseRequest(
            new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setValidityPeriod(new Period()
                    .setStartElement(new DateTimeType("2024-01-01"))
                )
        );

        var effectiveTimeXML = StatementTimeMappingUtils.prepareEffectiveTimeForMedicationRequest(medicationRequest);

        assertThat(effectiveTimeXML)
            .isEqualTo("<low value=\"20240101\"/>");
    }

    @Test
    void When_PrepareEffectiveTimeForMedicationRequestWithEffectivePeriodWithStartAndEnd_Expect_LowAndHighXmlElementsProduced() {
        medicationRequest.setDispenseRequest(
            new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setValidityPeriod(new Period()
                    .setStartElement(new DateTimeType("2024-01-01"))
                    .setEndElement(new DateTimeType("2024-02-02"))
                )
        );

        var effectiveTimeXML = StatementTimeMappingUtils.prepareEffectiveTimeForMedicationRequest(medicationRequest);

        assertThat(effectiveTimeXML)
            .isEqualTo("<low value=\"20240101\"/><high value=\"20240202\"/>");
    }

    @Test
    void When_PrepareAvailabilityTimeForMedicationRequestWithoutEffectivePeriod_Expect_EhrMapperExceptionContainingIdIsThrown() {
        medicationRequest.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent());

        assertThatEhrMapperExceptionWithIdThrown(
            () -> StatementTimeMappingUtils.prepareAvailabilityTimeForMedicationRequest(medicationRequest)
        );
    }

    @Test
    void When_PrepareAvailabilityTimeForMedicationRequestWithEffectivePeriodWithoutStart_Expect_EhrMapperExceptionContainingIdIsThrown() {
        medicationRequest.setDispenseRequest(
            new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setValidityPeriod(new Period())
        );

        assertThatEhrMapperExceptionWithIdThrown(
            () -> StatementTimeMappingUtils.prepareAvailabilityTimeForMedicationRequest(medicationRequest)
        );
    }

    @Test
    void When_PrepareAvailabilityTimeForMedicationRequestWithEffectivePeriodWithStart_Expect_AvailabilityTimeXmlElementProduced() {
        medicationRequest.setDispenseRequest(
            new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setValidityPeriod(new Period()
                    .setStartElement(new DateTimeType("2024-01-01"))
                )
        );

        var effectiveTimeXML = StatementTimeMappingUtils.prepareAvailabilityTimeForMedicationRequest(medicationRequest);

        assertThat(effectiveTimeXML)
            .isEqualTo("<availabilityTime value=\"20240101\"/>");
    }


    private void assertThatEhrMapperExceptionWithIdThrown(ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(EhrMapperException.class)
            .hasMessageContaining(MEDICATION_REQUEST_ID);
    }
}
