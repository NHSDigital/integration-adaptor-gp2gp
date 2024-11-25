package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectory;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.FileParsingUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import static org.mockito.ArgumentMatchers.anyList;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment;
import static uk.nhs.adaptors.gp2gp.utils.XmlAssertion.assertThatXml;
import static uk.nhs.adaptors.gp2gp.utils.XmlParsingUtility.getXmlStringFromFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiagnosticReportMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/";

    private static final String INPUT_JSON_BUNDLE = "fhir_bundle.json";
    private static final String INPUT_JSON_BUNDLE_WITH_FILING_COMMENTS = "fhir_bundle_with_filing_comments.json";

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";

    private static final String INPUT_JSON_REQUIRED_DATA = "diagnostic-report-with-required-data.json";
    private static final String INPUT_JSON_EMPTY_SPECIMENS = "diagnostic-report-with-empty-specimens.json";
    private static final String INPUT_JSON_EMPTY_RESULTS = "diagnostic-report-with-empty-results.json";
    private static final String INPUT_JSON_ONE_SPECIMEN = "diagnostic-report-with-one-specimen.json";
    private static final String INPUT_JSON_ONE_RESULT = "diagnostic-report-with-one-result.json";
    private static final String INPUT_JSON_MULTI_SPECIMENS = "diagnostic-report-with-multi-specimens.json";
    private static final String INPUT_JSON_MULTI_RESULTS = "diagnostic-report-with-multi-results.json";
    private static final String INPUT_JSON_PERFORMER = "diagnostic-report-with-performer.json";
    private static final String INPUT_JSON_PERFORMER_NO_ACTOR = "diagnostic-report-with-performer-no-actor.json";
    private static final String INPUT_JSON_CONCLUSION = "diagnostic-report-with-conclusion.json";
    private static final String INPUT_JSON_CODED_DIAGNOSIS = "diagnostic-report-with-coded-diagnosis.json";
    private static final String INPUT_JSON_MULTIPLE_CODED_DIAGNOSIS = "diagnostic-report-with-multiple-coded-diagnosis.json";
    private static final String INPUT_JSON_EXTENSION_ID = "diagnostic-report-with-extension-id.json";
    private static final String INPUT_JSON_URN_OID_EXTENSION_ID = "diagnostic-report-with-urn-oid-extension-id.json";

    private static final String OUTPUT_XML_REQUIRED_DATA = "diagnostic-report-with-required-data.xml";
    private static final String OUTPUT_XML_STATUS_NARRATIVE = "diagnostic-report-with-status-narrative.xml";
    private static final String OUTPUT_XML_ONE_SPECIMEN = "diagnostic-report-with-one-specimen.xml";
    private static final String OUTPUT_XML_MULTI_SPECIMENS = "diagnostic-report-with-multi-specimens.xml";
    private static final String OUTPUT_XML_PARTICIPANT = "diagnostic-report-with-participant.xml";
    private static final String OUTPUT_XML_CONCLUSION = "diagnostic-report-with-conclusion.xml";
    private static final String OUTPUT_XML_CODED_DIAGNOSIS = "diagnostic-report-with-coded-diagnosis.xml";
    private static final String OUTPUT_XML_MULTIPLE_CODED_DIAGNOSIS = "diagnostic-report-with-multiple-coded-diagnosis.xml";
    private static final String OUTPUT_XML_EXTENSION_ID = "diagnostic-report-with-extension-id.xml";
    private static final String OUTPUT_XML_MULTIPLE_RESULTS = "diagnostic-report-with-multiple-results.xml";

    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private SpecimenMapper specimenMapper;
    @Mock
    private MessageContext messageContext;
    @Mock
    private ConfidentialityService confidentialityService;
    @Mock
    private IdMapper idMapper;
    @Mock
    private AgentDirectory agentDirectory;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private DiagnosticReportMapper mapper;

    @BeforeEach
    public void setUp() throws IOException {
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE);

        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(messageContext.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenAnswer(mockIdForResourceAndId());
        when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockIdForReference());
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(specimenMapper.mapSpecimenToCompoundStatement(any(Specimen.class), anyList(), any(DiagnosticReport.class)))
            .thenAnswer(mockSpecimenMapping());
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        mapper = new DiagnosticReportMapper(
            messageContext, specimenMapper, new ParticipantMapper(), randomIdGeneratorService, confidentialityService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    void When_MappingDiagnosticReportJson_Expect_CompoundStatementXmlOutput(String inputJson, String outputXml) {
        final CharSequence expectedOutputMessage = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + outputXml);
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(inputJson);

        final String outputMessage = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage.toString());
    }

    @Test
    void When_DiagnosticReport_With_BlankFilingComment_Expect_NoNarrativeStatementPresent() {
        final String diagnosticReportFileName = "diagnostic-report-with-blank-filing-comment.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE_WITH_FILING_COMMENTS);
        final InputBundle inputBundle = new InputBundle(bundle);

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        diagnosticReport.setStatus(null);

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertThat(actualXml)
            .doesNotContainPattern("<NarrativeStatement\\s+classCode=\"[^\"]+\"\\s+moodCode=\"[^\"]+\">\n");
    }

    @Test
    void When_DiagnosticReport_With_NopatMetaSecurity_Expect_ConfidentialityCodeWithinCompoundStatement() {
        final String testFile = "diagnostic-report-with-multi-specimens-nopat.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(testFile);

        when(confidentialityService.generateConfidentialityCode(diagnosticReport))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final String result = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertAll(
            () -> assertThat(result).contains(NOPAT_HL7_CONFIDENTIALITY_CODE),
            () -> assertThat(ConfidentialityCodeUtility.getSecurityCodeFromResource(diagnosticReport)).isEqualTo("NOPAT")
        );
    }

    @Test
    void When_DiagnosticReport_With_NoscrubMetaSecurity_Expect_ConfidentialityCodeNotWithinCompoundStatement() {
        final String testFile = "diagnostic-report-with-multi-specimens-noscrub.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(testFile);

        when(confidentialityService.generateConfidentialityCode(diagnosticReport)).thenReturn(Optional.empty());

        final String result = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertAll(
            () -> assertThat(result).doesNotContain(NOPAT_HL7_CONFIDENTIALITY_CODE),
            () -> assertThat(ConfidentialityCodeUtility.getSecurityCodeFromResource(diagnosticReport)).isEqualTo("NOSCRUB")
        );
    }

    @Test
    void When_DiagnosticReport_With_RedactedFilingComment_Expect_ConfidentialityCodePresentWithinUserCommentNarrativeStatement() {
        final String diagnosticReportFileName = "diagnostic-report-with-one-result.json";
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE_WITH_FILING_COMMENTS);
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final InputBundle inputBundle = new InputBundle(bundle);

        when(confidentialityService.generateConfidentialityCode(bundle.getEntry().getFirst().getResource()))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));
        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        diagnosticReport.setStatus(null);

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertThat(actualXml).containsIgnoringWhitespaces(NOPAT_HL7_CONFIDENTIALITY_CODE);
    }

    @Test
    void When_DiagnosticReport_With_ObservationEffectiveDateTimeTypeAndCommentNote_Expect_NarrativeStatementConfidentialityCodePresent() {
        final String diagnosticReportFileName = "diagnostic-report-with-effective-datetime-filing-comment.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE_WITH_FILING_COMMENTS);
        final int observationBundleEntryIndex = 2;
        final Observation observation = (Observation) bundle.getEntry().get(observationBundleEntryIndex).getResource();
        final InputBundle inputBundle = new InputBundle(bundle);
        final List<String> expectedXPaths = Collections.singletonList(
            "/component/CompoundStatement/component[1]/NarrativeStatement/" + getNopatConfidentialityCodeXpathSegment()
        );

        when(confidentialityService.generateConfidentialityCode(observation)).thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));
        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        diagnosticReport.setStatus(null);

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertThatXml(actualXml).containsAllXPaths(expectedXPaths);
    }

    @Test
    void When_DiagnosticReport_With_ObservationEffectivePeriodAndCommentNote_Expect_NarrativeStatementConfidentialityCodePresent() {
        final String diagnosticReportFileName = "diagnostic-report-with-effective-period-filing-comment.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE_WITH_FILING_COMMENTS);
        final int observationBundleEntryIndex = 3;
        final Observation observation = (Observation) bundle.getEntry().get(observationBundleEntryIndex).getResource();
        final InputBundle inputBundle = new InputBundle(bundle);
        final List<String> expectedXPaths = Collections.singletonList(
            "/component/CompoundStatement/component[1]/NarrativeStatement/" + getNopatConfidentialityCodeXpathSegment()
        );

        when(confidentialityService.generateConfidentialityCode(observation)).thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));
        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        diagnosticReport.setStatus(null);

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertThatXml(actualXml).containsAllXPaths(expectedXPaths);
    }

    @Test
    void When_DiagnosticReport_With_NoReferencedSpecimenAndFilingCommentWithNoComment_Expect_MatchesSnapshotXml() {
        final String diagnosticReportFileName = "diagnostic-report-with-no-specimen.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE);
        final InputBundle inputBundle = new InputBundle(bundle);
        final String expectedXml = getXmlStringFromFile(TEST_FILE_DIRECTORY, "diagnostic-report-with-no-specimen.xml");

        when(specimenMapper.mapSpecimenToCompoundStatement(
            any(Specimen.class),
            anyList(),
            any(DiagnosticReport.class)
        )).thenCallRealMethod();

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        mapper = new DiagnosticReportMapper(
            messageContext,
            new SpecimenMapper(
                messageContext,
                new ObservationMapper(
                    messageContext,
                    new StructuredObservationValueMapper(),
                    codeableConceptCdMapper,
                    new ParticipantMapper(),
                    randomIdGeneratorService,
                    confidentialityService
                ),
                randomIdGeneratorService,
                confidentialityService
            ),
            new ParticipantMapper(),
            randomIdGeneratorService,
            confidentialityService
        );

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        assertThat(actualXml).isEqualToIgnoringWhitespace(expectedXml);
    }

    /**
     * A Diagnosis Report may have an Observation (Test Result) and Specimen. If the result and specimen are not
     * linked then we need to create a dummy specimen linked to the result.
     */
    @Test
    void When_DiagnosticReport_Has_SpecimenAndUnlinkedTestResult_Expect_ADummySpecimenLinkedToTestResult() {
        final String diagnosticReportFileName = "diagnostic-report-with-one-specimen-and-one-unrelated-observation.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE);
        final InputBundle inputBundle = new InputBundle(bundle);

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

        // This checks that the unlinked test result is given a dummy specimen.
        assertThat(actualXml).containsIgnoringWhitespaces(
                "<!-- Mapped Specimen with id: DUMMY-SPECIMEN-5E496953-065B-41F2-9577-BE8F2FBD0757 "
                        + "with linked Observations: Observation/TestResult-WithoutSpecimenReference-->");
    }

    @Test
    void When_DiagnosticReport_Has_SpecimenALinkedTestResultAndAnUnlinkedTestResult_Expect_ASpecimenOnAllTestResults() {
        final String diagnosticReportFileName =
                "diagnostic-report-with-one-specimen-one-linked-observation-and-one-unlinked-observation.json";
        final DiagnosticReport diagnosticReport = getDiagnosticReportResourceFromJson(diagnosticReportFileName);
        final Bundle bundle = getBundleResourceFromJson(INPUT_JSON_BUNDLE);
        final InputBundle inputBundle = new InputBundle(bundle);
        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        final String actualXml = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);
        // This checks that the unlinked test result is given a dummy specimen.
        assertThat(actualXml).containsIgnoringWhitespaces(
                "<!-- Mapped Specimen with id: DUMMY-SPECIMEN-5E496953-065B-41F2-9577-BE8F2FBD0757 "
                        + "with linked Observations: Observation/TestResult-WithoutSpecimenReference-->");

    }

    private Bundle getBundleResourceFromJson(String filename) {
        final String filePath = TEST_FILE_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, Bundle.class);
    }

    private DiagnosticReport getDiagnosticReportResourceFromJson(String filename) {
        final String filePath = TEST_FILE_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, DiagnosticReport.class);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_REQUIRED_DATA, OUTPUT_XML_STATUS_NARRATIVE),
            Arguments.of(INPUT_JSON_EMPTY_SPECIMENS, OUTPUT_XML_STATUS_NARRATIVE),
            Arguments.of(INPUT_JSON_EMPTY_RESULTS, OUTPUT_XML_STATUS_NARRATIVE),
            Arguments.of(INPUT_JSON_ONE_SPECIMEN, OUTPUT_XML_ONE_SPECIMEN),
            Arguments.of(INPUT_JSON_ONE_RESULT, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_MULTI_SPECIMENS, OUTPUT_XML_MULTI_SPECIMENS),
            Arguments.of(INPUT_JSON_MULTI_RESULTS, OUTPUT_XML_MULTIPLE_RESULTS),
            Arguments.of(INPUT_JSON_PERFORMER, OUTPUT_XML_PARTICIPANT),
            Arguments.of(INPUT_JSON_PERFORMER_NO_ACTOR, OUTPUT_XML_STATUS_NARRATIVE),
            Arguments.of(INPUT_JSON_CONCLUSION, OUTPUT_XML_CONCLUSION),
            Arguments.of(INPUT_JSON_CODED_DIAGNOSIS, OUTPUT_XML_CODED_DIAGNOSIS),
            Arguments.of(INPUT_JSON_MULTIPLE_CODED_DIAGNOSIS, OUTPUT_XML_MULTIPLE_CODED_DIAGNOSIS),
            Arguments.of(INPUT_JSON_EXTENSION_ID, OUTPUT_XML_EXTENSION_ID),
            Arguments.of(INPUT_JSON_URN_OID_EXTENSION_ID, OUTPUT_XML_EXTENSION_ID)
        );
    }

    private Answer<String> mockIdForResourceAndId() {
        return invocation -> {
            ResourceType resourceType = invocation.getArgument(0);
            String originalId = invocation.getArgument(1).toString();
            return String.format("II-for-%s-%s", resourceType.name(), originalId);
        };
    }

    private Answer<String> mockIdForReference() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("II-for-%s", reference.getReference());
        };
    }

    private Answer<String> mockSpecimenMapping() {
        return invocation -> {
            Specimen specimen = invocation.getArgument(0);
            List<Observation> observations = invocation.getArgument(1);

            List<String> linkedObservations = new ArrayList<>();

            for (Observation observation : observations) {
                if (observation.getSpecimen().getReference() != null
                        && observation.getSpecimen().getReference().equals(specimen.getId())) {
                    linkedObservations.add(observation.getId());
                }
            }

            if (linkedObservations.isEmpty()) {
                return String.format("<!-- Mapped Specimen with id: %s -->", specimen.getId());
            }
            return String.format("<!-- Mapped Specimen with id: %s with linked Observations: %s-->",
                    specimen.getId(),
                    String.join(",", linkedObservations));
        };
    }
}