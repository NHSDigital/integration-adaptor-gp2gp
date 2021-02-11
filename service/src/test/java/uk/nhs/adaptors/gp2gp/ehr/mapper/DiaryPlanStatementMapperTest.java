package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public class DiaryPlanStatementMapperTest extends MapperTest {

    private static final String TEST_ID = "394559384658936";

    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/procedurerequest/";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_ALL_DATA = TEST_FILE_DIRECTORY + "procedure-request-resource-1.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_ALL_DATA = TEST_FILE_DIRECTORY + "expected-plan-statement-1.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_NO_DATA = TEST_FILE_DIRECTORY + "procedure-request-resource-2.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_EMPTY_DATA = TEST_FILE_DIRECTORY + "expected-plan-statement-2.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_DEVICE = TEST_FILE_DIRECTORY + "procedure-request-resource-3.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_DEVICE = TEST_FILE_DIRECTORY + "expected-plan-statement-3.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_PRACTITIONER = TEST_FILE_DIRECTORY + "procedure-request-resource-4.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_PRACTITIONER = TEST_FILE_DIRECTORY + "expected-plan-statement-4.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_REASON_DISPLAY = TEST_FILE_DIRECTORY + "procedure-request-resource-5.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_REASON_DISPLAY = TEST_FILE_DIRECTORY + "expected-plan-statement-5.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITH_PERIOD_END = TEST_FILE_DIRECTORY + "procedure-request-resource-6.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_PERIOD_END = TEST_FILE_DIRECTORY + "expected-plan-statement-6.xml";
    private static final String INPUT_PROCEDURE_REQUEST_WITHOUT_PERIOD_END = TEST_FILE_DIRECTORY + "procedure-request-resource-7.json";
    private static final String EXPECTED_PLAN_STATEMENT_WITHOUT_PERIOD_END = TEST_FILE_DIRECTORY + "expected-plan-statement-7.xml";
    private static final String EXPECTED_PLAN_STATEMENT_WITH_IS_NESTED = TEST_FILE_DIRECTORY + "expected-plan-statement-8.xml";
    private static final String INPUT_PROCEDURE_REQUEST_IS_NOT_PLAN = TEST_FILE_DIRECTORY + "procedure-request-resource-0.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private MessageContext messageContext;
    private DiaryPlanStatementMapper diaryPlanStatementMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        diaryPlanStatementMapper = new DiaryPlanStatementMapper(messageContext);
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

        String mappedXml = diaryPlanStatementMapper.mapDiaryProcedureRequestToPlanStatement(inputProcedureRequest, true);
        assertThat(mappedXml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingProcedureRequestThatIsNotPlan_Expect_ResourceNotMapped() throws IOException {
        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_PROCEDURE_REQUEST_IS_NOT_PLAN);
        ProcedureRequest inputProcedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);

        String mappedXml = diaryPlanStatementMapper.mapDiaryProcedureRequestToPlanStatement(inputProcedureRequest, true);
        assertThat(mappedXml).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void When_MappingProcedureRequest_Expect_ResourceMapped(String inputJsonPath, String expectedXmlPath) throws IOException {
        String expectedXml = ResourceTestFileUtils.getFileContent(expectedXmlPath);

        String inputJson = ResourceTestFileUtils.getFileContent(inputJsonPath);
        ProcedureRequest inputProcedureRequest = new FhirParseService().parseResource(inputJson, ProcedureRequest.class);

        String mappedXml = diaryPlanStatementMapper.mapDiaryProcedureRequestToPlanStatement(inputProcedureRequest, false);
        assertThat(mappedXml).isEqualToIgnoringWhitespace(expectedXml);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_ALL_DATA, EXPECTED_PLAN_STATEMENT_WITH_ALL_DATA),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_NO_DATA, EXPECTED_PLAN_STATEMENT_WITH_EMPTY_DATA),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_DEVICE, EXPECTED_PLAN_STATEMENT_WITH_DEVICE),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_PRACTITIONER, EXPECTED_PLAN_STATEMENT_WITH_PRACTITIONER),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_REASON_DISPLAY, EXPECTED_PLAN_STATEMENT_WITH_REASON_DISPLAY),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITH_PERIOD_END, EXPECTED_PLAN_STATEMENT_WITH_PERIOD_END),
            Arguments.of(INPUT_PROCEDURE_REQUEST_WITHOUT_PERIOD_END, EXPECTED_PLAN_STATEMENT_WITHOUT_PERIOD_END)
        );
    }
}
