package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
public class ConditionLinkSetMapperTest {

    private static final String CONDITION_ID = "7E277DF1-6F1C-47CD-84F7-E9B7BF4105DB-PROB";
    private static final String GENERATED_ID = "50233a2f-128f-4b96-bdae-6207ed11a8ea";

    private static final String CONDITION_FILE_LOCATIONS = "/ehr/mapper/condition/";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION = CONDITION_FILE_LOCATIONS + "condition_all_included.json";
    private static final String INPUT_JSON_NO_ACTUAL_PROBLEM = CONDITION_FILE_LOCATIONS + "condition_no_problem.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION = CONDITION_FILE_LOCATIONS
        + "condition_actual_problem_condition.json";

    private static final String EXPECTED_OUTPUT_LINKSET = CONDITION_FILE_LOCATIONS + "expected_output_linkset_";
    private static final String OUTPUT_XML_WITH_IS_NESTED = EXPECTED_OUTPUT_LINKSET + "1.xml";
    private static final String OUTPUT_XML_WITHOUT_IS_NESTED = EXPECTED_OUTPUT_LINKSET + "2.xml";
    private static final String OUTPUT_XML_NO_ACTUAL_PROBLEM = EXPECTED_OUTPUT_LINKSET + "3.xml";
    private static final String OUTPUT_XML_WITH_CONDITION_NAMED= EXPECTED_OUTPUT_LINKSET + "4.xml";
    private static final String OUTPUT_XML_WITH_CONDITION_NAMED_OBSERVATION_STATEMENT_GENERATED= EXPECTED_OUTPUT_LINKSET + "5.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private IdMapper idMapper;
    @Mock
    private MessageContext messageContext;
    private ConditionLinkSetMapper conditionLinkSetMapper;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() throws IOException {
        fhirParseService = new FhirParseService();
        conditionLinkSetMapper = new ConditionLinkSetMapper(messageContext);
        when(messageContext.getIdMapper()).thenReturn(idMapper);
        //when(idMapper.getNew()).thenReturn(GENERATED_ID);
        when(idMapper.getOrNew(ResourceType.Condition, CONDITION_ID)).thenReturn(CONDITION_ID);
        when(idMapper.getOrNew(any(Reference.class))).thenAnswer(answerWithObjectId());
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void When_MappingParsedConditionWithRealProblem_Expect_LinkSetXml(String conditionJson, String outputXml, boolean isNested) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(conditionJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        Condition condition = fhirParseService.parseResource(jsonInput, Condition.class);

        String outputMessage = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);
        System.out.println(outputMessage);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION, OUTPUT_XML_WITH_IS_NESTED, true),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION, OUTPUT_XML_WITHOUT_IS_NESTED, false),
            Arguments.of(INPUT_JSON_NO_ACTUAL_PROBLEM, OUTPUT_XML_NO_ACTUAL_PROBLEM, false),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION, OUTPUT_XML_WITH_CONDITION_NAMED, false)
            //Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION, OUTPUT_XML_WITH_CONDITION_NAMED_OBSERVATION_STATEMENT_GENERATED, false)
        );
    }



    private Answer<String> answerWithObjectId() {
        return invocation -> {
           Reference reference = invocation.getArgument(0);
            return reference.getReferenceElement().getIdPart();
        };
    }
}
