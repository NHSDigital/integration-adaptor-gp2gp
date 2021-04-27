package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class MedicationStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/medication_request/";
    private static final String INPUT_JSON_BUNDLE =  TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_INVALID_INTENT = TEST_FILE_DIRECTORY + "medication-request-with-invalid-intent.json";
    private static final String INPUT_JSON_WITH_NO_VALIDITY_PERIOD = TEST_FILE_DIRECTORY
        + "medication-request-with-no-validity-period.json";
    private static final String INPUT_JSON_WITH_INVALID_PRESCRIPTION_TYPE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-prescription-type.json";
    private static final String INPUT_JSON_WITH_INVALID_BASED_ON_MEDICATION_REFERENCE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-based-on-medication-reference.json";
    private static final String INPUT_JSON_WITH_INVALID_BASED_ON_MEDICATION_REFERENCE_TYPE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-based-on-medication-reference-type.json";
    private static final String INPUT_JSON_WITH_INVALID_PRIOR_PRESCRIPTION_MEDICATION_REFERENCE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-prior-prescription-medication-reference.json";
    private static final String INPUT_JSON_WITH_INVALID_PRIOR_PRESCRIPTION_MEDICATION_REFERENCE_TYPE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-prior-prescription-medication-reference-type.json";
    private static final String INPUT_JSON_WITH_NO_STATUS = TEST_FILE_DIRECTORY + "medication-request-with-no-status.json";
    private static final String INPUT_JSON_WITH_NO_DOSAGE_INSTRUCTION = TEST_FILE_DIRECTORY
        + "medication-request-with-no-dosage-instruction.json";
    private static final String INPUT_JSON_WITH_NO_DISPENSE_REQUEST = TEST_FILE_DIRECTORY
        + "medication-request-with-no-dispense-request.json";
    private static final String INPUT_JSON_WITH_ORDER_NO_BASED_ON = TEST_FILE_DIRECTORY
        + "medication-request-with-order-no-based-on.json";
    private static final String INPUT_JSON_WITH_PLAN_STATUS_REASON_STOPPED_NO_DATE = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-status-reason-stopped-no-date.json";
    private static final String INPUT_JSON_WITH_ORDER_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-request-with-order-no-optional-fields.json";
    private static final String OUTPUT_XML_WITH_PRESCRIBE_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-statement-with-prescribe-no-optional-fields.xml";
    private static final String INPUT_JSON_WITH_ORDER_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-request-with-order-optional-fields.json";
    private static final String OUTPUT_XML_WITH_PRESCRIBE_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-statement-with-prescribe-optional-fields.xml";
    private static final String INPUT_JSON_WITH_COMPLETE_STATUS = TEST_FILE_DIRECTORY + "medication-request-with-complete-status.json";
    private static final String OUTPUT_XML_WITH_COMPLETE_STATUS = TEST_FILE_DIRECTORY + "medication-statement-with-complete-status.xml";
    private static final String INPUT_JSON_WITH_ACTIVE_STATUS = TEST_FILE_DIRECTORY + "medication-request-with-active-status.json";
    private static final String OUTPUT_XML_WITH_ACTIVE_STATUS = TEST_FILE_DIRECTORY + "medication-statement-with-active-status.xml";
    private static final String INPUT_JSON_WITH_PLAN_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-optional-fields.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-optional-fields.xml";
    private static final String INPUT_JSON_WITH_DISPENSE_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-dispense-quantity-text.json";
    private static final String OUTPUT_XML_WITH_DISPENSE_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-statement-with-dispense-quantity-text.xml";
    private static final String INPUT_JSON_WITH_NO_DISPENSE_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-dispense-quantity-text.json";
    private static final String OUTPUT_XML_WITH_NO_DISPENSE_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-statement-with-no-dispense-quantity-text.xml";
    private static final String INPUT_JSON_WITH_NO_DISPENSE_QUANTITY_VALUE = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-dispense-quantity-value.json";
    private static final String OUTPUT_XML_WITH_NO_DISPENSE_QUANTITY_VALUE = TEST_FILE_DIRECTORY
        + "medication-statement-with-no-dispense-quantity-value.xml";
    private static final String INPUT_JSON_WITH_PLAN_ACUTE_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-acute-prescription.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_ACUTE_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-acute-prescription.xml";
    private static final String INPUT_JSON_WITH_PLAN_REPEAT_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-repeat-prescription.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-repeat-prescription.xml";
    private static final String INPUT_JSON_WITH_ORDER_REPEAT_PRESCRIPTION_NO_VALUE = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-repeat-prescription-no-value.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION_NO_VALUE = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-repeat-prescription-no-value.xml";
    private static final String INPUT_JSON_WITH_PLAN_START_PERIOD_ONLY = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-start-period-only.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_START_PERIOD_ONLY = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-start-period-only.xml";
    private static final String INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-optional-fields.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-no-optional-fields.xml";
    private static final String INPUT_JSON_WITH_ORDER_BASED_ON = TEST_FILE_DIRECTORY
        + "medication-request-with-order-based-on.json";
    private static final String OUTPUT_XML_WITH_PRESCRIBE_BASED_ON = TEST_FILE_DIRECTORY
        + "medication-statement-with-prescribe-based-on.xml";
    private static final String OUTPUT_XML_WITH_AUTHORISE_PRIOR_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-statement-with-prescribe-prior-prescription.xml";
    private static final String INPUT_JSON_WITH_PLAN_NO_STATUS_REASON_CODE = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-status-reason-code.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_DEFAULT_STATUS_REASON_CODE = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-default-status-reason-code.xml";
    private static final String INPUT_JSON_WITH_PLAN_NO_INFO_PRESCRIPTION_TEXT = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-info-prescription-text.json";
    private static final String INPUT_JSON_WITH_NO_RECORDER_REFERENCE = TEST_FILE_DIRECTORY
        + "medication-request-with-no-recorder-reference.json";

    @Mock
    private RandomIdGeneratorService mockRandomIdGeneratorService;

    private MessageContext messageContext;
    private CodeableConceptCdMapper codeableConceptCdMapper;
    private MedicationStatementMapper medicationStatementMapper;

    @BeforeEach
    public void setUp() throws IOException {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        messageContext = new MessageContext(mockRandomIdGeneratorService);
        messageContext.initialize(bundle);
        messageContext.getIdMapper().getOrNew(ResourceType.Practitioner, "1");
        messageContext.getIdMapper().getOrNew(ResourceType.Organization, "2");
        messageContext.getIdMapper().getOrNew(ResourceType.PractitionerRole, "3");
        medicationStatementMapper = new MedicationStatementMapper(messageContext, codeableConceptCdMapper,
            new ParticipantMapper(), mockRandomIdGeneratorService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        assertThatInputMapsToExpectedOutput(inputJson, outputXml);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_ORDER_NO_OPTIONAL_FIELDS, OUTPUT_XML_WITH_PRESCRIBE_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_ORDER_OPTIONAL_FIELDS, OUTPUT_XML_WITH_PRESCRIBE_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_COMPLETE_STATUS, OUTPUT_XML_WITH_COMPLETE_STATUS),
            Arguments.of(INPUT_JSON_WITH_ACTIVE_STATUS, OUTPUT_XML_WITH_ACTIVE_STATUS),
            Arguments.of(INPUT_JSON_WITH_PLAN_OPTIONAL_FIELDS, OUTPUT_XML_WITH_AUTHORISE_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_DISPENSE_QUANTITY_TEXT, OUTPUT_XML_WITH_DISPENSE_QUANTITY_TEXT),
            Arguments.of(INPUT_JSON_WITH_NO_DISPENSE_QUANTITY_TEXT, OUTPUT_XML_WITH_NO_DISPENSE_QUANTITY_TEXT),
            Arguments.of(INPUT_JSON_WITH_NO_DISPENSE_QUANTITY_VALUE, OUTPUT_XML_WITH_NO_DISPENSE_QUANTITY_VALUE),
            Arguments.of(INPUT_JSON_WITH_PLAN_ACUTE_PRESCRIPTION, OUTPUT_XML_WITH_AUTHORISE_ACUTE_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_PLAN_REPEAT_PRESCRIPTION, OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_ORDER_REPEAT_PRESCRIPTION_NO_VALUE, OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION_NO_VALUE),
            Arguments.of(INPUT_JSON_WITH_PLAN_START_PERIOD_ONLY, OUTPUT_XML_WITH_AUTHORISE_START_PERIOD_ONLY),
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS, OUTPUT_XML_WITH_AUTHORISE_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_STATUS_REASON_CODE, OUTPUT_XML_WITH_AUTHORISE_DEFAULT_STATUS_REASON_CODE),
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_INFO_PRESCRIPTION_TEXT, OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION)
        );
    }

    @SneakyThrows
    private void assertThatInputMapsToExpectedOutput(String inputJsonResourcePath, String outputXmlResourcePath) {
        var expected = ResourceTestFileUtils.getFileContent(outputXmlResourcePath);
        var input = ResourceTestFileUtils.getFileContent(inputJsonResourcePath);
        var parsedMedicationRequest = new FhirParseService().parseResource(input, MedicationRequest.class);

        String outputMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        assertThat(outputMessage).isEqualTo(expected);
    }

    @SneakyThrows
    @Test
    public void When_MappingBasedOnField_Expect_CorrectReferences() {
        var expected = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_PRESCRIBE_BASED_ON);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("123");
        var inputAuthorise1 = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PLAN_ACUTE_PRESCRIPTION);
        var parsedMedicationRequest1 = new FhirParseService().parseResource(inputAuthorise1, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest1);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("456", "456", "123", "789");
        var inputWithBasedOn = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_ORDER_BASED_ON);
        var parsedMedicationRequestWithBasedOn = new FhirParseService().parseResource(inputWithBasedOn, MedicationRequest.class);
        String outputMessageWithBasedOn =
            medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequestWithBasedOn);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("789");
        var inputAuthorise2 = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PLAN_REPEAT_PRESCRIPTION);
        var parsedMedicationRequest2 = new FhirParseService().parseResource(inputAuthorise2, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest2);

        assertThat(outputMessageWithBasedOn).isEqualTo(expected);
    }

    @SneakyThrows
    @Test
    public void When_MappingPriorPrescriptionField_Expect_CorrectReferences() {
        var expected = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_AUTHORISE_PRIOR_PRESCRIPTION);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("123");
        var inputAuthorise = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PLAN_ACUTE_PRESCRIPTION);
        var parsedMedicationRequest = new FhirParseService().parseResource(inputAuthorise, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("456", "456", "123");
        var inputWithPriorPrescription = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PLAN_OPTIONAL_FIELDS);
        var parsedMedicationRequestWithPriorPrescription =
            new FhirParseService().parseResource(inputWithPriorPrescription, MedicationRequest.class);
        String outputMessageWithPriorPrescription =
            medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequestWithPriorPrescription);

        assertThat(outputMessageWithPriorPrescription).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("resourceFileExpectException")
    public void When_MappingMedicationRequestWithInvalidResource_Expect_Exception(String inputJson) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);

        RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();
        medicationStatementMapper = new MedicationStatementMapper(messageContext, codeableConceptCdMapper,
            new ParticipantMapper(), randomIdGeneratorService);

        assertThrows(EhrMapperException.class, ()
            -> medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest));
    }

    private static List<String> resourceFileExpectException() {
        return List.of(
            INPUT_JSON_WITH_NO_VALIDITY_PERIOD,
            INPUT_JSON_WITH_INVALID_INTENT,
            INPUT_JSON_WITH_INVALID_PRESCRIPTION_TYPE,
            INPUT_JSON_WITH_INVALID_BASED_ON_MEDICATION_REFERENCE,
            INPUT_JSON_WITH_INVALID_BASED_ON_MEDICATION_REFERENCE_TYPE,
            INPUT_JSON_WITH_INVALID_PRIOR_PRESCRIPTION_MEDICATION_REFERENCE,
            INPUT_JSON_WITH_INVALID_PRIOR_PRESCRIPTION_MEDICATION_REFERENCE_TYPE,
            INPUT_JSON_WITH_NO_STATUS,
            INPUT_JSON_WITH_NO_DOSAGE_INSTRUCTION,
            INPUT_JSON_WITH_NO_DISPENSE_REQUEST,
            INPUT_JSON_WITH_ORDER_NO_BASED_ON,
            INPUT_JSON_WITH_PLAN_STATUS_REASON_STOPPED_NO_DATE,
            INPUT_JSON_WITH_NO_RECORDER_REFERENCE
            );
    }
}
