package uk.nhs.adaptors.gp2gp.ehr.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public class MedicationRequestUtilsTest {

    private final FhirParseService fhirParseService = new FhirParseService();

    private static final String FILES_ROOT = "/ehr/utils/";
    private static final String JSON_SUPPRESSED_MEDICATION_REQUEST = FILES_ROOT
        + "medication-request-suppressed.json";
    private static final String JSON_NOT_SUPPRESSED_MEDICATION_REQUEST = FILES_ROOT
        + "medication-request-not-suppressed.json";
    private static final String JSON_CONDITION_WITH_MEDICATION_REQUEST_REF = FILES_ROOT
        + "condition_with_medication_request_reference.json";

    @SneakyThrows
    @Test
    public void When_MedicationRequestIsSuppressed_Expect_True() {
        var jsonInput = ResourceTestFileUtils.getFileContent(JSON_SUPPRESSED_MEDICATION_REQUEST);
        var medicationRequest = fhirParseService.parseResource(jsonInput, MedicationRequest.class);
        boolean isSuppressed = MedicationRequestUtils.isMedicationRequestSuppressed(medicationRequest);

        assertTrue(isSuppressed);
    }

    @SneakyThrows
    @Test
    public void When_MedicationRequestIsNotSuppressed_Expect_False() {
        var jsonInput = ResourceTestFileUtils.getFileContent(JSON_NOT_SUPPRESSED_MEDICATION_REQUEST);
        var medicationRequest = fhirParseService.parseResource(jsonInput, MedicationRequest.class);
        boolean isSuppressed = MedicationRequestUtils.isMedicationRequestSuppressed(medicationRequest);

        assertFalse(isSuppressed);
    }

    @SneakyThrows
    @Test
    public void When_AskingIfMedRequestReference_IsOfTypeMedicationRequest_Expect_Ture() {
        var jsonInput = ResourceTestFileUtils.getFileContent(JSON_CONDITION_WITH_MEDICATION_REQUEST_REF);
        var condition = fhirParseService.parseResource(jsonInput, Condition.class);
        var reference = (Reference) condition.getExtension()
            .stream().findFirst().get().getValue();

        boolean isMedicationRequestReference = MedicationRequestUtils.isMedicationRequestType(reference);

        assertTrue(isMedicationRequestReference);
    }

    @SneakyThrows
    @Test
    public void When_AskingIfNonMedRequestReference_IsOfTypeMedicationRequest_Expect_False() {
        var jsonInput = ResourceTestFileUtils.getFileContent(JSON_CONDITION_WITH_MEDICATION_REQUEST_REF);
        var condition = fhirParseService.parseResource(jsonInput, Condition.class);
        var reference = (Reference) condition.getExtension()
            .stream().skip(1).findFirst().get().getValue();

        boolean isMedicationRequestReference = MedicationRequestUtils.isMedicationRequestType(reference);

        assertFalse(isMedicationRequestReference);
    }
}
