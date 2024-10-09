package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@MockitoSettings(strictness = Strictness.LENIENT)
public class DiaryPlanStatementMapperTest {

    private static final String TEST_ID = "394559384658936";

    private static final String TEST_DIRECTORY = "/ehr/mapper/procedurerequest/";
    private static final String INPUT_PROCEDURE_REQUEST_IS_NOT_PLAN = TEST_DIRECTORY + "procedure-request-resource-0.json";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_ALL_DATA = TEST_DIRECTORY + "procedure-request-resource-1.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_ALL_DATA = TEST_DIRECTORY + "expected-plan-statement-1.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_NO_OPTIONAL_DATA = TEST_DIRECTORY + "procedure-request-resource-2.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_EMPTY_DATA = TEST_DIRECTORY + "expected-plan-statement-2.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_PRACTITIONER = TEST_DIRECTORY + "procedure-request-resource-4.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_PRACTITIONER = TEST_DIRECTORY + "expected-plan-statement-4.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_MULTIPLE_REASON_CODES = TEST_DIRECTORY + "procedure-request-resource-5.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_MULTIPLE_REASON_CODES = TEST_DIRECTORY + "expected-plan-statement-5.xml";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_IS_NESTED = TEST_DIRECTORY + "expected-plan-statement-8.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITHOUT_REQUIRED_AUTHORED_ON = TEST_DIRECTORY + "procedure-request-resource-8.json";
    private static final String INPUT_PROCEDURE_REQUEST_SINGLE_REASON_CODE = TEST_DIRECTORY + "procedure-request-resource-9.json";
    private static final String EXPECTED_PROCEDURE_REQUEST_SINGLE_REASON_CODE = TEST_DIRECTORY + "expected-plan-statement-9.xml";
    private static final String INPUT_JSON_WITH_SINGLE_SUPPORTING_INFO = TEST_DIRECTORY
        + "procedure-request-resource-with-single-supportInfo.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_SUPPORTING_INFO = TEST_DIRECTORY
        + "procedure-request-resource-with-multiple-supportInfo.json";
    private static final String OUTPUT_JSON_WITH_SINGLE_SUPPORTING_INFO = TEST_DIRECTORY
        + "expected-output-procedure-request-resource-with-single-supportInfo.xml";
    private static final String OUTPUT_JSON_WITH_MULTIPLE_SUPPORTING_INFO = TEST_DIRECTORY
        + "expected-output-procedure-request-resource-with-multiple-supportInfo.xml";
    private static final String INPUT_BUNDLE = TEST_DIRECTORY + "input-bundle.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private MessageContext messageContext;
    private DiaryPlanStatementMapper diaryPlanStatementMapper;

    @BeforeEach
    public void setUp() throws IOException {
        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(inputJson, Bundle.class);

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);

        diaryPlanStatementMapper =
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper, new ParticipantMapper());
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingProcedureRequestIsNested_Expect_ResourceMappedWithIsNestedFlag() throws IOException {
        String expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_PLAN_STATEMENT_WITH_IS_NESTED);

        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_PROCEDURE_REQUEST_WITH_ALL_DATA);
        ProcedureRequest inputProcedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);

        var mappedXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(inputProcedureRequest, true);
        assertThat(mappedXml).contains(expectedXml);
    }

    @Test
    public void When_MappingProcedureRequestThatIsNotPlan_Expect_ResourceNotMapped() throws IOException {
        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_PROCEDURE_REQUEST_IS_NOT_PLAN);
        ProcedureRequest inputProcedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);

        var mappedXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(inputProcedureRequest, true);
        assertThat(mappedXml).isNull();
    }

    @Test
    public void When_MappingProcedureRequestWithoutRequiredAuthoredOn_Expect_MapperException() throws IOException {
        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_PROCEDURE_REQUEST_WITHOUT_REQUIRED_AUTHORED_ON);
        ProcedureRequest inputProcedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);

        assertThrows(EhrMapperException.class, ()
            -> diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(inputProcedureRequest, true));
    }

    @Test
    public void When_MappingWithOccurrenceWithStartAndEnd_Expect_TextContainsEarliestRecallDateOfStartAndEffectiveTimeOfEnd() {
        var inputJson = """
            {
                "resourceType": "ProcedureRequest",
                "id": "1B1C6F50-D8BE-4480-83D6-EF25AC5F1836",
                "status": "active",
                "intent": "plan",
                "authoredOn": "2010-01-13T15:29:50.1+00:00",
                "occurrencePeriod": {
                    "start": "2010-01-13",
                    "end": "2010-01-15"
                },
                "code": {
                    "text": "test"
                }
            }""";

        final var expectedXml = """
            <component typeCode="COMP" >
                <PlanStatement classCode="OBS" moodCode="INT">
                    <id root="394559384658936" />
                    <code nullFlavor="UNK"><originalText>Mocked code</originalText></code>
                    <text>
                        Earliest Recall Date: 2010-01-13
                    </text>
                    <statusCode code="COMPLETE" />
                    <effectiveTime>
                        <center value="20100115"/>
                    </effectiveTime>
                    <availabilityTime value="20100113152950"/>
                </PlanStatement>
            </component>""";

        final var procedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);
        var actualXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(procedureRequest, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingWithOccurrenceWithOnlyStart_Expect_TextDoesNotContainEarliestRecallDateAndEffectiveTimeOfStart() {
        var inputJson = """
            {
                "resourceType": "ProcedureRequest",
                "id": "1B1C6F50-D8BE-4480-83D6-EF25AC5F1836",
                "status": "active",
                "intent": "plan",
                "authoredOn": "2010-01-13T15:29:50.1+00:00",
                "occurrencePeriod": {
                    "start": "2010-01-13"
                },
                "code": {
                    "text": "test"
                }
            }""";

        final var expectedXml = """
                <component typeCode="COMP" >
                    <PlanStatement classCode="OBS" moodCode="INT">
                        <id root="394559384658936" />
                        <code nullFlavor="UNK"><originalText>Mocked code</originalText></code>
                        <statusCode code="COMPLETE" />
                        <effectiveTime>
                            <center value="20100113"/>
                        </effectiveTime>
                        <availabilityTime value="20100113152950"/>
                    </PlanStatement>
                </component>""";

        final var procedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);
        var actualXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(procedureRequest, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingWithDeviceReferenceWhereDeviceHasManufacturer_Expect_TextContainsDeviceTypeTextAndManufacturer() {
        var inputJson = """
            {
                "resourceType": "ProcedureRequest",
                "id": "1B1C6F50-D8BE-4480-83D6-EF25AC5F1836",
                "status": "active",
                "intent": "plan",
                "authoredOn": "2010-01-13T15:29:50.1+00:00",
                "requester": {
                    "agent": {
                        "reference": "Device/device-with-manufacturer"
                    }
                },
                "code": {
                    "text": "test"
                }
            }""";

        final var expectedXml = """
            <component typeCode="COMP" >
                <PlanStatement classCode="OBS" moodCode="INT">
                    <id root="394559384658936" />
                    <code nullFlavor="UNK"><originalText>Mocked code</originalText></code>
                    <text>
                        Recall Device: DeviceTypeText DeviceManufacturer
                    </text>
                    <statusCode code="COMPLETE" />
                    <effectiveTime>
                        <center nullFlavor="UNK"/>
                    </effectiveTime>
                    <availabilityTime value="20100113152950"/>
                </PlanStatement>
            </component>""";

        final var procedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);
        var actualXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(procedureRequest, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingWithDeviceReferenceWhereDeviceHasNoManufacturer_Expect_TextOnlyContainsDeviceTypeText() {
        var inputJson = """
            {
                "resourceType": "ProcedureRequest",
                "id": "1B1C6F50-D8BE-4480-83D6-EF25AC5F1836",
                "status": "active",
                "intent": "plan",
                "authoredOn": "2010-01-13T15:29:50.1+00:00",
                "requester": {
                    "agent": {
                        "reference": "Device/device-without-manufacturer"
                    }
                },
                "code": {
                    "text": "test"
                }
            }""";

        final var expectedXml = """
            <component typeCode="COMP" >
                <PlanStatement classCode="OBS" moodCode="INT">
                    <id root="394559384658936" />
                    <code nullFlavor="UNK"><originalText>Mocked code</originalText></code>
                    <text>
                        Recall Device: DeviceTypeText
                    </text>
                    <statusCode code="COMPLETE" />
                    <effectiveTime>
                        <center nullFlavor="UNK"/>
                    </effectiveTime>
                    <availabilityTime value="20100113152950"/>
                </PlanStatement>
            </component>""";

        final var procedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);
        var actualXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(procedureRequest, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void When_MappingProcedureRequest_Expect_ResourceMapped(String inputJsonPath, String expectedXmlPath) throws IOException {
        String expectedXml = ResourceTestFileUtils.getFileContent(expectedXmlPath);

        String inputJson = ResourceTestFileUtils.getFileContent(inputJsonPath);
        ProcedureRequest inputProcedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);

        var mappedXml = diaryPlanStatementMapper.mapProcedureRequestToPlanStatement(inputProcedureRequest, false);
        assertThat(mappedXml).contains(expectedXml);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_ALL_DATA, EXPECTED_PLAN_STATEMENT_WITH_ALL_DATA),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_NO_OPTIONAL_DATA, EXPECTED_PLAN_STATEMENT_WITH_EMPTY_DATA),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_PRACTITIONER, EXPECTED_PLAN_STATEMENT_WITH_PRACTITIONER),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_MULTIPLE_REASON_CODES, EXPECTED_PLAN_STATEMENT_WITH_MULTIPLE_REASON_CODES),
            Arguments.of(INPUT_PROCEDURE_REQUEST_SINGLE_REASON_CODE, EXPECTED_PROCEDURE_REQUEST_SINGLE_REASON_CODE),
            Arguments.of(INPUT_JSON_WITH_SINGLE_SUPPORTING_INFO, OUTPUT_JSON_WITH_SINGLE_SUPPORTING_INFO),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_SUPPORTING_INFO, OUTPUT_JSON_WITH_MULTIPLE_SUPPORTING_INFO)
        );
    }
}
