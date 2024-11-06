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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectory;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.FileParsingUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;
import static uk.nhs.adaptors.gp2gp.utils.XmlAssertion.assertThatXml;

@ExtendWith(MockitoExtension.class)
class ObservationMapperTest {
    public static final String COMPOUND_STATEMENT_CONFIDENTIALITY_CODE_XPATH =
        "/component/CompoundStatement/" + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();
    public static final String OBSERVATION_STATEMENT_CONFIDENTIALITY_CODE_XPATH =
        "/component/ObservationStatement/" + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();
    public static final String NARRATIVE_STATEMENT_CONFIDENTIALITY_CODE_XPATH =
        "/component/NarrativeStatement/" + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();
    private static final String DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/";
    private static final String OBSERVATION_TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/observation/";

    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_JSON =
        "observation_associated_with_specimen_1.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_2_JSON =
        "observation_associated_with_specimen_2.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_3_JSON =
        "observation_associated_with_specimen_3.json";
    private static final String OBSERVATION_WITH_MULTIPLE_INTERPRETATIONS_JSON =
        "observation_with_multiple_interpretations.json";
    private static final String OBSERVATION_WITH_INTERPRETATION_CODE_LOW_JSON =
        "observation_with_interpretation_code_low.json";
    private static final String OBSERVATION_WITH_INTERPRETATION_CODE_ABNORMAL_JSON =
        "observation_with_interpretation_code_abnormal.json";
    private static final String OBSERVATION_WITH_DATA_ABSENT_REASON_AND_INTERPRETATION_AND_BODY_SITE_AND_METHOD_JSON =
        "observation_with_data_absent_reason_and_interpretation_and_body_site_and_method.json";
    private static final String OBSERVATION_WITH_VALUE_QUANTITY_AND_REFERENCE_RANGE_JSON =
        "observation_with_value_quantity_and_reference_range.json";
    private static final String OBSERVATION_WITH_REFERENCE_RANGE_AND_INTERPRETATION_JSON =
        "observation_with_reference_range_and_interpretation.json";
    private static final String OBSERVATION_WITH_EFFECTIVEPERIOD_START_AND_NO_EFFECTIVEDATETIME_JSON =
        "observation_with_effectiveperiod_start_and_no_effectivedatetime.json";
    private static final String OBSERVATION_WITH_NO_EFFECTIVEPERIOD_AND_NO_EFFECTIVEDATETIME_JSON =
        "observation_with_no_effectiveperiod_and_no_effectivedatetime.json";
    private static final String OBSERVATION_WITH_REFERENCERANGE_AND_NO_VALUEQUANTITY_JSON =
        "observation_with_referencerange_and_no_valuequantity.json";
    private static final String OBSERVATION_WITH_VALUE_STRING_JSON =
        "observation_with_value_string.json";
    private static final String OBSERVATION_REFERENCED_BY_DIAGNOSTICREPORT_RESULT_JSON =
        "observation_referenced_by_diagnosticreport_result.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_WITH_RELATED_COMMENT_JSON =
        "observation_associated_with_specimen_1_with_related_comment.json";
    private static final String OBSERVATION_TEST_GROUP_HEADER_JSON =
        "observation_test_group_header.json";
    private static final String OBSERVATION_TEST_RESULT_JSON =
        "observation_test_result.json";
    private static final String OBSERVATION_FILING_COMMENT_JSON =
        "observation_filing_comment.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_IGNORED_MEMBER_JSON =
        "observation_associated_with_specimen_1_with_ignored_member.json";

    private static final String OBSERVATION_COMPOUND_STATEMENT_1_XML =
        "observation_compound_statement_1.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_2_XML =
        "observation_compound_statement_2.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_3_XML =
        "observation_compound_statement_3.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_MULTIPLE_INTERPRETATIONS_XML =
        "observation_compound_statement_with_multiple_interpretations.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_INTERPRETATION_CODE_LOW_XML =
        "observation_compound_statement_with_interpretation_code_low.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_INTERPRETATION_CODE_ABNORMAL_XML =
        "observation_compound_statement_with_interpretation_code_abnormal.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_DATA_ABSENT_REASON_AND_INTERPRETATION_AND_BODY_SITE_AND_METHOD_XML =
        "observation_compound_statement_with_data_absent_reason_and_interpretation_and_body_site_and_method.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_VALUE_QUANTITY_AND_REFERENCE_RANGE_XML =
        "observation_compound_statement_with_value_quantity_and_reference_range.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_REFERENCE_RANGE_AND_INTERPRETATION_XML =
        "observation_compound_statement_with_reference_range_and_interpretation.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_AVAILABILITYTIME_AND_LOW_EFFECTIVETIME_XML =
        "observation_compound_statement_with_availabilitytime_and_low_effectivetime.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_NO_EFFECTIVE_PROPERLY_HANDLED =
        "observation_compound_statement_with_no_effective_properly_handled.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_REFERENCERANGE_IN_COMMENT =
        "observation_compound_statement_with_referencerange_in_comment.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_VALUE_STRING_OUTPUT =
        "observation_compound_statement_with_value_string.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_CLUSTERED_BY_DIAGNOSTICREPORT_REFERENCE_XML =
        "observation_compound_statement_clustered_by_diagnosticreport_reference.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_1_WITH_RELATED_COMMENT_XML =
        "observation_compound_statement_1_with_related_comment.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_WITH_BATTERY_TEST_RESULT_XML =
        "observation_compound_statement_with_battery_test_result.xml";
    private static final String DIAGNOSTIC_REPORT_REFERENCE_ID = "Observation/TEST_REFERENCE_ID";

    @Mock
    private IdMapper idMapper;
    @Mock
    private AgentDirectory agentDirectory;
    @Mock
    private MessageContext messageContext;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private ConfidentialityService confidentialityService;
    @Captor
    private ArgumentCaptor<Observation> observationArgumentCaptor;

    private ObservationMapper observationMapper;

    @BeforeEach
    void setUp() {
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

        observationMapper = new ObservationMapper(
            messageContext,
            new StructuredObservationValueMapper(),
            new CodeableConceptCdMapper(),
            new ParticipantMapper(),
            randomIdGeneratorService,
            confidentialityService
        );
    }

    @AfterEach
    void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    void When_MappingObservationJson_Expect_CompoundStatementXmlOutput(String inputJson, String outputXml) {
        final Observation observationAssociatedWithSpecimen = getObservationResourceFromJson(inputJson);
        final String expectedXml = getXmlStringFromFile(outputXml);

        lenient().when(randomIdGeneratorService.createNewId())
            .thenReturn("random-unmapped-id");

        final String actualXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen);

        assertThat(actualXml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    void When_MappingDefaultObservationJson_Expect_DefaultObservationStatementXmlOutput() {
        final Observation observationAssociatedWithSpecimen =
            getObservationResourceFromJson("input_default_observation.json");
        final String expectedXml = getXmlStringFromFile("expected_output_default_observation.xml");

        final String actualXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingTestGroupHeader_With_NopatMetaSecurity_Expect_ConfidentialityCodeWithinCompoundStatement() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_TEST_GROUP_HEADER_JSON);

        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observationArgumentCaptor.capture()))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final String actualXml = observationMapper.mapObservationToCompoundStatement(observation);

        final var metaWithNopat = observationArgumentCaptor.getAllValues().stream()
            .map(Observation::getMeta)
            .filter(ConfidentialityCodeUtility::doesMetaContainNopat)
            .toList();

        assertAll(
            () -> assertThatXml(actualXml).containsXPath(COMPOUND_STATEMENT_CONFIDENTIALITY_CODE_XPATH),
            () -> assertThat(metaWithNopat).hasSize(2)
        );
    }

    @Test
    void When_MappingTestGroupHeader_WithoutCode_Expect_ExceptionThrownContainingId() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_TEST_GROUP_HEADER_JSON);
        observation.setCode(null);

        assertThatExceptionOfType(EhrMapperException.class)
            .isThrownBy(() -> observationMapper.mapObservationToCompoundStatement(observation))
            .withMessageContaining(observation.getId());

    }

    @Test
    void When_MappingTestGroupHeader_With_NoscrubMetaSecurity_Expect_ConfidentialityCodeNotPresent() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_TEST_GROUP_HEADER_JSON);

        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observationArgumentCaptor.capture()))
            .thenReturn(Optional.empty());

        final String actualXml = observationMapper.mapObservationToCompoundStatement(observation);

        final var metaWithNopat = observationArgumentCaptor.getAllValues().stream()
            .map(Observation::getMeta)
            .filter(ConfidentialityCodeUtility::doesMetaContainNopat)
            .toList();

        assertAll(
            () -> assertThat(actualXml).doesNotContainIgnoringCase(NOPAT_HL7_CONFIDENTIALITY_CODE),
            () -> assertThat(metaWithNopat).hasSize(0)
        );
    }

    @Test
    void When_MappingTestResult_With_NopatMetaSecurity_Expect_ConfidentialityCodeWithinObservationStatement() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_TEST_RESULT_JSON);

        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observation))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final String actualXml = observationMapper.mapObservationToCompoundStatement(observation);

        assertThatXml(actualXml).containsXPath(OBSERVATION_STATEMENT_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_MappingTestResult_With_NoscrubMetaSecurity_Expect_ConfidentialityCodeNotPresent() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_TEST_RESULT_JSON);

        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observation))
            .thenReturn(Optional.empty());

        final String actualXml = observationMapper.mapObservationToCompoundStatement(observation);

        assertThat(actualXml).doesNotContainIgnoringCase(NOPAT_HL7_CONFIDENTIALITY_CODE);
    }

    @Test
    void When_MappingFilingComment_With_NopatMetaSecurity_Expect_ConfidentialityCodeWithinNarrativeStatement() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_FILING_COMMENT_JSON);

        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observation))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final String actualXml = observationMapper.mapObservationToCompoundStatement(observation);

        assertThatXml(actualXml).containsXPath(NARRATIVE_STATEMENT_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_MappingFilingComment_With_NopatMetaSecurity_Expect_ConfidentialityCodeNotPresent() {
        final Observation observation = getObservationResourceFromJson(OBSERVATION_FILING_COMMENT_JSON);

        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observation))
            .thenReturn(Optional.empty());

        final String actualXml = observationMapper.mapObservationToCompoundStatement(observation);

        assertThat(actualXml).doesNotContainIgnoringCase(NOPAT_HL7_CONFIDENTIALITY_CODE);
    }

    private String getXmlStringFromFile(String filename) {
        return ResourceTestFileUtils.getFileContent(
            OBSERVATION_TEST_FILE_DIRECTORY + filename
        );
    }

    private Observation getObservationResourceFromJson(String filename) {
        final String filePath = OBSERVATION_TEST_FILE_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, Observation.class);
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
            ),
            Arguments.of(
                OBSERVATION_ASSOCIATED_WITH_IGNORED_MEMBER_JSON,
                OBSERVATION_COMPOUND_STATEMENT_WITH_BATTERY_TEST_RESULT_XML
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