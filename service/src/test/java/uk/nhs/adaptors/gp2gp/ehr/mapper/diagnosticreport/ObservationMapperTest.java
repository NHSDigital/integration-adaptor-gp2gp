package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
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

import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectory;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class ObservationMapperTest {
    private static final String DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/";
    private static final String OBSERVATION_TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/observation/";

    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_1.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_2_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_2.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_3_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_3.json";
    private static final String OBSERVATION_WITH_MULTIPLE_INTERPRETATIONS_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_multiple_interpretations.json";
    private static final String OBSERVATION_WITH_INTERPRETATION_CODE_LOW_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_interpretation_code_low.json";
    private static final String OBSERVATION_WITH_INTERPRETATION_CODE_ABNORMAL_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_interpretation_code_abnormal.json";
    private static final String OBSERVATION_WITH_DATA_ABSENT_REASON_AND_INTERPRETATION_AND_BODY_SITE_AND_METHOD_JSON =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_with_data_absent_reason_and_interpretation_and_body_site_and_method.json";
    private static final String OBSERVATION_WITH_VALUE_QUANTITY_AND_REFERENCE_RANGE_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_value_quantity_and_reference_range.json";
    private static final String OBSERVATION_WITHOUT_NARRATIVE_AND_RELATED = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_associated_without_narrative_and_related.json";

    private static final String OBSERVATION_COMPOUND_STATEMENT_1_XML = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_compound_statement_1.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_2_XML = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_compound_statement_2.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_3_XML = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_compound_statement_3.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_MULTIPLE_INTERPRETATIONS_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_multiple_interpretations.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_INTERPRETATION_CODE_LOW_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_interpretation_code_low.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_INTERPRETATION_CODE_ABNORMAL_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_interpretation_code_abnormal.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_DATA_ABSENT_REASON_AND_INTERPRETATION_AND_BODY_SITE_AND_METHOD_XML =
        OBSERVATION_TEST_FILE_DIRECTORY
            + "observation_compound_statement_with_data_absent_reason_and_interpretation_and_body_site_and_method.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_VALUE_QUANTITY_AND_REFERENCE_RANGE_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_value_quantity_and_reference_range.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_DUMMY_NARRATIVE_STMT = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_compound_dummy_narrative_stmt.xml";

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";

    @Mock
    private IdMapper idMapper;

    @Mock
    private AgentDirectory agentDirectory;

    @Mock
    private MessageContext messageContext;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private ObservationMapper observationMapper;

    @BeforeEach
    public void setUp() throws IOException {
        String bundleJsonInput = ResourceTestFileUtils.getFileContent(DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "fhir_bundle.json");
        Bundle bundle = new FhirParseService().parseResource(bundleJsonInput, Bundle.class);
        InputBundle inputBundle = new InputBundle(bundle);
        lenient().when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);
        lenient().when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        lenient().when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockReference());
        lenient().when(agentDirectory.getAgentRef(any(Reference.class), any(Reference.class))).thenAnswer(mockReferences());

        when(messageContext.getIdMapper()).thenReturn(idMapper);

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        observationMapper = new ObservationMapper(
            messageContext,
            new StructuredObservationValueMapper(),
            new CodeableConceptCdMapper(),
            new ParticipantMapper(),
            randomIdGeneratorService
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_CompoundStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");

        String jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation observationAssociatedWithSpecimen = new FhirParseService().parseResource(jsonInput, Observation.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(outputXml);

        String compoundStatementXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen
        );

        assertThat(compoundStatementXml).isEqualTo(expectedXmlOutput);
    }

    @Test
    public void When_MappingDefaultObservationJson_Expect_DefaultObservationStatementXmlOutput() throws IOException {
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");

        String jsonInput = ResourceTestFileUtils.getFileContent(
            OBSERVATION_TEST_FILE_DIRECTORY + "input_default_observation.json"
        );
        Observation observationAssociatedWithSpecimen = new FhirParseService().parseResource(jsonInput, Observation.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
            OBSERVATION_TEST_FILE_DIRECTORY + "expected_output_default_observation.xml"
        );

        String compoundStatementXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen
        );

        assertThat(compoundStatementXml).isEqualTo(expectedXmlOutput);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_JSON, OBSERVATION_COMPOUND_STATEMENT_1_XML),
            Arguments.of(OBSERVATION_ASSOCIATED_WITH_SPECIMEN_2_JSON, OBSERVATION_COMPOUND_STATEMENT_2_XML),
            Arguments.of(OBSERVATION_ASSOCIATED_WITH_SPECIMEN_3_JSON, OBSERVATION_COMPOUND_STATEMENT_3_XML),
            Arguments.of(OBSERVATION_WITH_MULTIPLE_INTERPRETATIONS_JSON, OBSERVATION_COMPOUND_STATEMENT_WITH_MULTIPLE_INTERPRETATIONS_XML),
            Arguments.of(OBSERVATION_WITH_INTERPRETATION_CODE_LOW_JSON, OBSERVATION_COMPOUND_STATEMENT_WITH_INTERPRETATION_CODE_LOW_XML),
            Arguments.of(
                OBSERVATION_WITH_INTERPRETATION_CODE_ABNORMAL_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_INTERPRETATION_CODE_ABNORMAL_XML
            ),
            Arguments.of(
                OBSERVATION_WITH_DATA_ABSENT_REASON_AND_INTERPRETATION_AND_BODY_SITE_AND_METHOD_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_DATA_ABSENT_REASON_AND_INTERPRETATION_AND_BODY_SITE_AND_METHOD_XML
            ),
            Arguments.of(
                OBSERVATION_WITH_VALUE_QUANTITY_AND_REFERENCE_RANGE_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_VALUE_QUANTITY_AND_REFERENCE_RANGE_XML
            ),
            Arguments.of(
                OBSERVATION_WITHOUT_NARRATIVE_AND_RELATED,
                OBSERVATION_COMPOUND_STATEMENT_DUMMY_NARRATIVE_STMT)
        );
    }

    private Answer<String> mockReference() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("REFERENCE-to-%s", reference.getReference());
        };
    }

    private Answer<String> mockReferences() {
        return invocation -> {
            Reference practitionerReference = invocation.getArgument(0);
            Reference organizationReference = invocation.getArgument(1);
            return String.format("REFERENCE-to-%s-%s", practitionerReference.getReference(), organizationReference.getReference());
        };
    }
}
