package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
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

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
    private static final String OBSERVATION_WITH_REFERENCE_RANGE_AND_INTERPRETATION_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_reference_range_and_interpretation.json";
    private static final String OBSERVATION_WITH_EFFECTIVEPERIOD_START_AND_NO_EFFECTIVEDATETIME_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_effectiveperiod_start_and_no_effectivedatetime.json";
    private static final String OBSERVATION_WITH_NO_EFFECTIVEPERIOD_AND_NO_EFFECTIVEDATETIME_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_no_effectiveperiod_and_no_effectivedatetime.json";
    private static final String OBSERVATION_WITH_REFERENCERANGE_AND_NO_VALUEQUANTITY_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_referencerange_and_no_valuequantity.json";
    private static final String OBSERVATION_WITH_VALUE_STRING_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_with_value_string.json";
    private static final String OBSERVATION_REFERENCED_BY_DIAGNOSTICREPORT_RESULT_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_referenced_by_diagnosticreport_result.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_WITH_RELATED_COMMENT_JSON = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_1_with_related_comment.json";


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
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_REFERENCE_RANGE_AND_INTERPRETATION_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_reference_range_and_interpretation.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_AVAILABILITYTIME_AND_LOW_EFFECTIVETIME_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_availabilitytime_and_low_effectivetime.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_NO_EFFECTIVE_PROPERLY_HANDLED =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_no_effective_properly_handled.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_REFERENCERANGE_IN_COMMENT =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_referencerange_in_comment.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_VALUE_STRING_OUTPUT =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_with_value_string.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_CLUSTERED_BY_DIAGNOSTICREPORT_REFERENCE_XML =
        OBSERVATION_TEST_FILE_DIRECTORY + "observation_compound_statement_clustered_by_diagnosticreport_reference.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_1_WITH_RELATED_COMMENT_XML = OBSERVATION_TEST_FILE_DIRECTORY
        + "observation_compound_statement_1_with_related_comment.xml";

    private static final String DIAGNOSTIC_REPORT_REFERENCE_ID = "Observation/TEST_REFERENCE_ID";

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

        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(
            new DiagnosticReport().addResult(
                new Reference().setReference(DIAGNOSTIC_REPORT_REFERENCE_ID)
            )
        ));

        InputBundle inputBundle = new InputBundle(bundle);
        lenient().when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);
        lenient().when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        lenient().when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockReference());
        lenient().when(agentDirectory.getAgentRef(any(Reference.class), any(Reference.class))).thenAnswer(mockReferences());

        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class)))
            .thenAnswer(params -> "Mapped-From-" + ((IdType) params.getArgument(1)).getValue());

        MultiStatementObservationHolderFactory multiStatementObservationHolderFactory =
            new MultiStatementObservationHolderFactory(messageContext, randomIdGeneratorService);

        observationMapper = new ObservationMapper(
            messageContext,
            new StructuredObservationValueMapper(),
            new CodeableConceptCdMapper(),
            new ParticipantMapper(),
            multiStatementObservationHolderFactory
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_CompoundStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn("random-unmapped-id");

        String jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation observationAssociatedWithSpecimen = new FhirParseService().parseResource(jsonInput, Observation.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(outputXml);

        String compoundStatementXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen
        );

        assertThat(compoundStatementXml).isEqualToIgnoringWhitespace(expectedXmlOutput);
    }

    @Test
    public void When_MappingDefaultObservationJson_Expect_DefaultObservationStatementXmlOutput() throws IOException {
        String jsonInput = ResourceTestFileUtils.getFileContent(
            OBSERVATION_TEST_FILE_DIRECTORY + "input_default_observation.json"
        );
        Observation observationAssociatedWithSpecimen = new FhirParseService().parseResource(jsonInput, Observation.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
            OBSERVATION_TEST_FILE_DIRECTORY + "expected_output_default_observation.xml"
        );

        String actualXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen
        );

        assertThat(actualXml).isEqualTo(expectedXmlOutput);
    }

    @SuppressWarnings("unused")
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
                OBSERVATION_WITH_REFERENCE_RANGE_AND_INTERPRETATION_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_REFERENCE_RANGE_AND_INTERPRETATION_XML
            ),
            Arguments.of(
                OBSERVATION_WITH_EFFECTIVEPERIOD_START_AND_NO_EFFECTIVEDATETIME_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_AVAILABILITYTIME_AND_LOW_EFFECTIVETIME_XML
            ),
            Arguments.of(
                OBSERVATION_WITH_NO_EFFECTIVEPERIOD_AND_NO_EFFECTIVEDATETIME_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_NO_EFFECTIVE_PROPERLY_HANDLED
            ),
            Arguments.of(
                OBSERVATION_WITH_REFERENCERANGE_AND_NO_VALUEQUANTITY_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_REFERENCERANGE_IN_COMMENT
            ),
            Arguments.of(
                OBSERVATION_WITH_VALUE_STRING_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_VALUE_STRING_OUTPUT
            ),
            Arguments.of(
                OBSERVATION_REFERENCED_BY_DIAGNOSTICREPORT_RESULT_JSON,
                OBSERVATION_COMPOUND_STATEMENT_CLUSTERED_BY_DIAGNOSTICREPORT_REFERENCE_XML
            ),
            Arguments.of(
                OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_WITH_RELATED_COMMENT_JSON,
                OBSERVATION_COMPOUND_STATEMENT_1_WITH_RELATED_COMMENT_XML
            )
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