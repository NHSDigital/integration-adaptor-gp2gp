package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class MedicationStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/medication_request/";
    private static final String INPUT_JSON_BUNDLE = TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_BUNDLE_WITH_MEDICATION_STATEMENTS = TEST_FILE_DIRECTORY
        + "fhir-bundle-with-medication-statements.json";
    private static final String INPUT_JSON_WITH_INVALID_INTENT = TEST_FILE_DIRECTORY + "medication-request-with-invalid-intent.json";
    private static final String INPUT_JSON_WITH_NO_VALIDITY_PERIOD = TEST_FILE_DIRECTORY
        + "medication-request-with-no-validity-period.json";
    private static final String INPUT_JSON_WITH_INVALID_PRESCRIPTION_TYPE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-prescription-type.json";
    private static final String INPUT_JSON_WITH_INVALID_BASED_ON_MEDICATION_REFERENCE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-based-on-medication-reference.json";
    private static final String INPUT_JSON_WITH_INVALID_BASED_ON_MEDICATION_REFERENCE_TYPE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-based-on-medication-reference-type.json";
    private static final String INPUT_JSON_WITH_NO_STATUS = TEST_FILE_DIRECTORY + "medication-request-with-no-status.json";
    private static final String INPUT_JSON_WITH_NO_DOSAGE_INSTRUCTION = TEST_FILE_DIRECTORY
        + "medication-request-with-no-dosage-instruction.json";
    private static final String INPUT_JSON_WITH_NO_DISPENSE_REQUEST = TEST_FILE_DIRECTORY
        + "medication-request-with-no-dispense-request.json";
    private static final String INPUT_JSON_WITH_ORDER_NO_BASED_ON = TEST_FILE_DIRECTORY
        + "medication-request-with-order-no-based-on.json";
    private static final String INPUT_JSON_WITH_PLAN_STATUS_REASON_STOPPED_NO_DATE = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-status-reason-stopped-no-date.json";
    private static final String INPUT_JSON_WITH_INVALID_RECORDER_REFERENCE_TYPE = TEST_FILE_DIRECTORY
        + "medication-request-with-invalid-recorder-resource-type.json";
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
    private static final String INPUT_JSON_WITH_QUANTITY_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-quantity-quantity-text.json";
    private static final String OUTPUT_XML_WITH_DISPENSE_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-statement-with-dispense-quantity-text.xml";
    private static final String OUTPUT_XML_WITH_QUANTITY_QUANTITY_TEXT = TEST_FILE_DIRECTORY
        + "medication-statement-with-quantity-quantity-text.xml";
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
    private static final String INPUT_JSON_WITH_PLAN_NO_STATUS_REASON_CODE = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-status-reason-code.json";
    private static final String OUTPUT_XML_WITH_AUTHORISE_DEFAULT_STATUS_REASON_CODE = TEST_FILE_DIRECTORY
        + "medication-statement-with-authorise-default-status-reason-code.xml";
    private static final String INPUT_JSON_WITH_PLAN_NO_INFO_PRESCRIPTION_TEXT = TEST_FILE_DIRECTORY
        + "medication-request-with-plan-no-info-prescription-text.json";
    private static final String INPUT_JSON_WITH_NO_RECORDER_REFERENCE = TEST_FILE_DIRECTORY
        + "medication-request-with-no-recorder-reference.json";
    private static final String INPUT_JSON_WITH_EXTENSION_STATUS_REASON_TEXT = TEST_FILE_DIRECTORY
        + "medication-request-with-extension-status-reason-with-text.json";
    private static final String OUTPUT_XML_WITH_STATUS_REASON_TEXT = TEST_FILE_DIRECTORY
        + "medication-statement-with-status-reason-text.xml";
    private static final String OUTPUT_XML_WITH_NO_PARTICIPANT = TEST_FILE_DIRECTORY
            + "medication-statement-with-no-participant.xml";
    private static final String INPUT_JSON_WITH_REQUESTER_ON_BEHALF_OF = TEST_FILE_DIRECTORY
        + "medication-request-with-requester-on-behalf-of.json";
    private static final String INPUT_JSON_WITH_REQUESTER = TEST_FILE_DIRECTORY
        + "medication-request-with-requester.json";
    private static final String INPUT_JSON_WITH_NO_REQUESTER = TEST_FILE_DIRECTORY
        + "medication-request-with-no-requester.json";
    private static final String INPUT_JSON_WITH_REQUESTER_AGENT_AS_ORG = TEST_FILE_DIRECTORY
        + "medication-request-with-requester-agent-as-org.json";
    private static final String INPUT_JSON_WITH_REQUESTER_ORG_AND_ON_BEHALF_OF = TEST_FILE_DIRECTORY
        + "medication-request-with-requester-org-and-on-behalf-of.json";
    private static final String INPUT_JSON_WITH_PRESCRIBED_BY_ANOTHER_ORG_IN_BUNDLE = TEST_FILE_DIRECTORY
        + "medication-request-prescribed-by-another-organisation.json";
    private static final String OUTPUT_XML_WITH_PRESCRIBED_BY_ANOTHER_ORG = TEST_FILE_DIRECTORY
        + "medication-statement-prescribed-by-another-organisation.xml";
    private static final String INPUT_JSON_WITH_PRESCRIBED_BY_GP_IN_BUNDLE = TEST_FILE_DIRECTORY
        + "medication-request-prescribed-by-gp-practice.json";
    private static final String OUTPUT_XML_NHS_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-statement-nhs-prescription.xml";
    private static final String INPUT_JSON_WITH_PRESCRIBED_BY_PREVIOUS_PRACTICE_IN_BUNDLE = TEST_FILE_DIRECTORY
        + "medication-request-prescribed-by-previous-practice.json";
    private static final String INPUT_JSON_WITH_PRESCRIBING_AGENCY_ERROR_EMPTY_CODING = TEST_FILE_DIRECTORY
        + "medication-request-empty-prescribing-agency-coding-array.json";
    private static final String INPUT_JSON_WITH_PRESCRIBING_AGENCY_ERROR_MISSING_CODEABLE_CONCEPT = TEST_FILE_DIRECTORY
        + "medication-request-missing-prescribing-agency-codeable-concept.json";
    private static final String CONFIDENTIALITY_CODE = """
        <confidentialityCode
            code="NOPAT"
            codeSystem="2.16.840.1.113883.4.642.3.47"
            displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
        />""";

    private static final String PRACTITIONER_RESOURCE_1 = "Practitioner/1";
    private static final String PRACTITIONER_RESOURCE_2 = "Practitioner/2";
    private static final String ORGANIZATION_RESOURCE_1 = "Organization/1";

    @Mock
    private RandomIdGeneratorService mockRandomIdGeneratorService;

    @Mock
    private ConfidentialityService confidentialityService;

    private MessageContext messageContext;
    private CodeableConceptCdMapper codeableConceptCdMapper;
    private MedicationStatementMapper medicationStatementMapper;

    @BeforeEach
    public void setUp() throws IOException {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        messageContext = new MessageContext(mockRandomIdGeneratorService);
        messageContext.initialize(bundle);
        messageContext.getAgentDirectory().getAgentId(new Reference(buildIdType(ResourceType.Practitioner, "1")));
        messageContext.getAgentDirectory().getAgentId(new Reference(buildIdType(ResourceType.Organization, "2")));
        medicationStatementMapper = new MedicationStatementMapper(
            messageContext,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );
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
            Arguments.of(INPUT_JSON_WITH_QUANTITY_QUANTITY_TEXT, OUTPUT_XML_WITH_QUANTITY_QUANTITY_TEXT),
            Arguments.of(INPUT_JSON_WITH_NO_DISPENSE_QUANTITY_TEXT, OUTPUT_XML_WITH_NO_DISPENSE_QUANTITY_TEXT),
            Arguments.of(INPUT_JSON_WITH_NO_DISPENSE_QUANTITY_VALUE, OUTPUT_XML_WITH_NO_DISPENSE_QUANTITY_VALUE),
            Arguments.of(INPUT_JSON_WITH_PLAN_ACUTE_PRESCRIPTION, OUTPUT_XML_WITH_AUTHORISE_ACUTE_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_PLAN_REPEAT_PRESCRIPTION, OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_ORDER_REPEAT_PRESCRIPTION_NO_VALUE, OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION_NO_VALUE),
            Arguments.of(INPUT_JSON_WITH_PLAN_START_PERIOD_ONLY, OUTPUT_XML_WITH_AUTHORISE_START_PERIOD_ONLY),
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS, OUTPUT_XML_WITH_AUTHORISE_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_STATUS_REASON_CODE, OUTPUT_XML_WITH_AUTHORISE_DEFAULT_STATUS_REASON_CODE),
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_INFO_PRESCRIPTION_TEXT, OUTPUT_XML_WITH_AUTHORISE_REPEAT_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_EXTENSION_STATUS_REASON_TEXT, OUTPUT_XML_WITH_STATUS_REASON_TEXT),
            Arguments.of(INPUT_JSON_WITH_NO_RECORDER_REFERENCE, OUTPUT_XML_WITH_NO_PARTICIPANT),
            Arguments.of(INPUT_JSON_WITH_INVALID_RECORDER_REFERENCE_TYPE, OUTPUT_XML_WITH_NO_PARTICIPANT)
        );
    }

    @SneakyThrows
    private void assertThatInputMapsToExpectedOutput(String inputJsonResourcePath, String outputXmlResourcePath) {
        var expected = ResourceTestFileUtils.getFileContent(outputXmlResourcePath);
        var input = ResourceTestFileUtils.getFileContent(inputJsonResourcePath);
        var parsedMedicationRequest = new FhirParseService().parseResource(input, MedicationRequest.class);

        String outputMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        assertThat(outputMessage).isEqualToNormalizingWhitespace(expected);
    }

    @SneakyThrows
    @Test
    public void When_MappingBasedOnField_Expect_CorrectReferences() {
        var expected = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_PRESCRIBE_BASED_ON);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("123");
        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn("456");


        var inputAuthorise1 = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PLAN_ACUTE_PRESCRIPTION);
        var parsedMedicationRequest1 = new FhirParseService().parseResource(inputAuthorise1, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest1);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("456", "123", "789");
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

    @ParameterizedTest
    @MethodSource("resourceFileExpectException")
    public void When_MappingMedicationRequestWithInvalidResource_Expect_Exception(String inputJson) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);

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
            INPUT_JSON_WITH_NO_STATUS,
            INPUT_JSON_WITH_NO_DOSAGE_INSTRUCTION,
            INPUT_JSON_WITH_NO_DISPENSE_REQUEST,
            INPUT_JSON_WITH_ORDER_NO_BASED_ON,
            INPUT_JSON_WITH_PLAN_STATUS_REASON_STOPPED_NO_DATE
        );
    }

    @Test
    public void When_MappingMedicationRequestWithRequesterWithOnBehalfOf_Expect_ParticipantMappedToAgent() throws IOException {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        var messageContextMock = mock(MessageContext.class);
        var agentDirectoryMock = mock(AgentDirectory.class);
        var idMapper = new IdMapper(mockRandomIdGeneratorService);
        var medicationRequestIdMapper = new MedicationRequestIdMapper(mockRandomIdGeneratorService);

        when(messageContextMock.getIdMapper()).thenReturn(idMapper);
        when(messageContextMock.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContextMock.getMedicationRequestIdMapper()).thenReturn(medicationRequestIdMapper);
        when(messageContextMock.getAgentDirectory()).thenReturn(agentDirectoryMock);

        medicationStatementMapper = new MedicationStatementMapper(
            messageContextMock,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_REQUESTER_ON_BEHALF_OF);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        ArgumentCaptor<Reference> agent = ArgumentCaptor.forClass(Reference.class);
        ArgumentCaptor<Reference> onBehalfOf = ArgumentCaptor.forClass(Reference.class);
        verify(agentDirectoryMock).getAgentRef(agent.capture(), onBehalfOf.capture());

        assertThat(agent.getValue().getReference()).isEqualTo(PRACTITIONER_RESOURCE_1);
        assertThat(onBehalfOf.getValue().getReference()).isEqualTo(ORGANIZATION_RESOURCE_1);
    }

    private static Stream<Arguments> resourceFilesWithParticipant() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_REQUESTER, PRACTITIONER_RESOURCE_1),
            Arguments.of(INPUT_JSON_WITH_NO_REQUESTER, PRACTITIONER_RESOURCE_2),
            Arguments.of(INPUT_JSON_WITH_REQUESTER_AGENT_AS_ORG, ORGANIZATION_RESOURCE_1),
            Arguments.of(INPUT_JSON_WITH_REQUESTER_ORG_AND_ON_BEHALF_OF, ORGANIZATION_RESOURCE_1)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceFilesWithParticipant")
    public void When_MappingMedicationRequestWithParticipant_Expect_ParticipantMappedToAgent(
        String inputJson, String agentId) throws IOException {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        var messageContextMock = mock(MessageContext.class);
        var agentDirectoryMock = mock(AgentDirectory.class);
        var idMapper = new IdMapper(mockRandomIdGeneratorService);
        var medicationRequestIdMapper = new MedicationRequestIdMapper(mockRandomIdGeneratorService);

        when(messageContextMock.getIdMapper()).thenReturn(idMapper);
        when(messageContextMock.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContextMock.getMedicationRequestIdMapper()).thenReturn(medicationRequestIdMapper);
        when(messageContextMock.getAgentDirectory()).thenReturn(agentDirectoryMock);

        medicationStatementMapper = new MedicationStatementMapper(
            messageContextMock,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );

        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        ArgumentCaptor<Reference> agent = ArgumentCaptor.forClass(Reference.class);
        verify(agentDirectoryMock).getAgentId(agent.capture());

        assertThat(agent.getValue().getReference()).isEqualTo(agentId);
    }

    private static Stream<Arguments> resourceFilesWithMedicationStatement() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_PRESCRIBED_BY_ANOTHER_ORG_IN_BUNDLE, OUTPUT_XML_WITH_PRESCRIBED_BY_ANOTHER_ORG),
            Arguments.of(INPUT_JSON_WITH_PRESCRIBED_BY_GP_IN_BUNDLE, OUTPUT_XML_NHS_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_PRESCRIBED_BY_PREVIOUS_PRACTICE_IN_BUNDLE, OUTPUT_XML_NHS_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_PRESCRIBING_AGENCY_ERROR_EMPTY_CODING, OUTPUT_XML_NHS_PRESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_PRESCRIBING_AGENCY_ERROR_MISSING_CODEABLE_CONCEPT, OUTPUT_XML_NHS_PRESCRIPTION)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceFilesWithMedicationStatement")
    public void When_MappingMedicationRequest_WithMedicationStatement_Expect_PrescribingAgencyMappedToSupplyType(
        String inputJson, String outputXml) throws IOException {

        var expected = ResourceTestFileUtils.getFileContent(outputXml);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE_WITH_MEDICATION_STATEMENTS);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        var messageContextMock = mock(MessageContext.class);
        var agentDirectoryMock = mock(AgentDirectory.class);
        var idMapper = new IdMapper(mockRandomIdGeneratorService);
        var medicationRequestIdMapper = new MedicationRequestIdMapper(mockRandomIdGeneratorService);

        when(messageContextMock.getIdMapper()).thenReturn(idMapper);
        when(messageContextMock.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContextMock.getMedicationRequestIdMapper()).thenReturn(medicationRequestIdMapper);
        when(messageContextMock.getAgentDirectory()).thenReturn(agentDirectoryMock);

        medicationStatementMapper = new MedicationStatementMapper(
            messageContextMock,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );

        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);
        var outputString = medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        assertXmlIsEqual(outputString, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS,
        INPUT_JSON_WITH_ORDER_NO_OPTIONAL_FIELDS
    })
    public void When_ConfidentialityServiceReturnsConfidentialityCode_Expect_MessageContainsConfidentialityCode(
        String inputJson
    ) {
        final var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        final var parsedMedicationRequest = new FhirParseService()
            .parseResource(jsonInput, MedicationRequest.class);
        when(confidentialityService.generateConfidentialityCode(parsedMedicationRequest))
            .thenReturn(Optional.of(CONFIDENTIALITY_CODE));

        final var actualMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(
            parsedMedicationRequest
        );

        assertThat(actualMessage)
            .contains(CONFIDENTIALITY_CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS,
        INPUT_JSON_WITH_ORDER_NO_OPTIONAL_FIELDS
    })
    public void When_ConfidentialityServiceReturnsEmptyOptional_Expect_MessageDoesNotContainConfidentialityCode(
        String inputJson
    ) {
        final var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        final var parsedMedicationRequest = new FhirParseService()
            .parseResource(jsonInput, MedicationRequest.class);
        when(confidentialityService.generateConfidentialityCode(parsedMedicationRequest))
            .thenReturn(Optional.empty());

        final var actualMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(
            parsedMedicationRequest
        );

        assertThat(actualMessage)
            .doesNotContain(CONFIDENTIALITY_CODE);
    }

    private void assertXmlIsEqual(String outputString, String expected) {

        Diff diff = DiffBuilder.compare(outputString).withTest(expected)
            .checkForIdentical()
            .ignoreWhitespace()
            .build();

        assertThat(diff.hasDifferences())
            .as("Xml is not equal: " + System.lineSeparator() + diff.fullDescription())
            .isFalse();
    }
}
