package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NonConsultationResourceMapperTest {
    private static final String FILES_DIRECTORY = "/ehr/mapper/non_consultation_bundles/";
    private static final String UNCATAGORISED_OBSERVATION_XML = FILES_DIRECTORY + "uncatagorised-observation-stub.xml";
    private static final String UNCATAGORISED_OBSERVATION_BUNDLE = FILES_DIRECTORY + "uncatagorised-observation-bundle.json";
    private static final String UNCATAGORISED_IGNORED_RESOURCE_BUNDLE = FILES_DIRECTORY + "uncatagorised-ignored-resource-bundle.json";
    private static final String EXPECTED_UNCATAGORISED_OBSERVATION_OUTPUT = FILES_DIRECTORY
        + "expected-uncatagorised-observation-output.xml";
    private static final String COMMENT_OBSERVATION_XML = FILES_DIRECTORY + "comment-observation-stub.xml";
    private static final String COMMENT_OBSERVATION_BUNDLE = FILES_DIRECTORY + "comment-observation-bundle.json";
    private static final String EXPECTED_COMMENT_OBSERVATION_OUTPUT = FILES_DIRECTORY + "expected-comment-observation-output.xml";
    private static final String IMMUNIZATION_XML = FILES_DIRECTORY + "immunization-stub.xml";
    private static final String IMMUNIZATION_BUNDLE = FILES_DIRECTORY + "immunization-bundle.json";
    private static final String EXPECTED_IMMUNIZATION_OUTPUT = FILES_DIRECTORY + "expected-immunization-output.xml";
    private static final String ALLERGY_INTOLERANCE_XML = FILES_DIRECTORY + "allergy-intolerance-stub.xml";
    private static final String ALLERGY_INTOLERANCE_BUNDLE = FILES_DIRECTORY + "allergy-intolerance-bundle.json";
    private static final String ENDED_ALLERGY_INTOLERANCE_BUNDLE = FILES_DIRECTORY + "ended-allergy-intolerance-bundle.json";
    private static final String EXPECTED_ALLERGY_INTOLERANCE_OUTPUT = FILES_DIRECTORY + "expected-allergy-intolerance-output.xml";
    private static final String MULTIPLE_ENDED_ALLERGY_INTOLERANCES_BUNDLE = FILES_DIRECTORY
        + "multiple-ended-allergy-intolerances-bundle.json";
    private static final String BLOOD_PRESSURE_XML = FILES_DIRECTORY + "blood-pressure-stub.xml";
    private static final String BLOOD_PRESSURE_BUNDLE = FILES_DIRECTORY + "blood-pressure-bundle.json";
    private static final String EXPECTED_BLOOD_PRESSURE_OUTPUT = FILES_DIRECTORY + "expected-blood-pressure-output.xml";
    private static final String REFERRAL_REQUEST_XML = FILES_DIRECTORY + "referral-request-stub.xml";
    private static final String REFERRAL_REQUEST_BUNDLE = FILES_DIRECTORY + "referral-request-bundle.json";
    private static final String EXPECTED_REFERRAL_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-referral-request-output.xml";
    private static final String MEDICATION_REQUEST_XML = FILES_DIRECTORY + "medication-request-stub.xml";
    private static final String MEDICATION_REQUEST_BUNDLE = FILES_DIRECTORY + "medication-request-bundle.json";
    private static final String EXPECTED_MEDICATION_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-medication-request-output.xml";
    private static final String CONDITION_XML = FILES_DIRECTORY + "condition-stub.xml";
    private static final String CONDITION_BUNDLE = FILES_DIRECTORY + "condition-bundle.json";
    private static final String EXPECTED_CONDITION_OUTPUT = FILES_DIRECTORY + "expected-condition-output.xml";
    private static final String PROCEDURE_REQUEST_XML = FILES_DIRECTORY + "procedure-request-stub.xml";
    private static final String PROCEDURE_REQUEST_BUNDLE = FILES_DIRECTORY + "procedure-request-bundle.json";
    private static final String EXPECTED_PROCEDURE_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-procedure-request-output.xml";
    private static final String DOCUMENT_REFERENCE_XML = FILES_DIRECTORY + "document-reference-stub.xml";
    private static final String DOCUMENT_REFERENCE_BUNDLE = FILES_DIRECTORY + "document-reference-bundle.json";
    private static final String EXPECTED_DOCUMENT_REFERENCE_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-document-reference-output.xml";
    private static final String DIAGNOSTIC_REPORT_XML = FILES_DIRECTORY + "diagnostic-report-stub.xml";
    private static final String DIAGNOSTIC_REPORT_BUNDLE = FILES_DIRECTORY + "diagnostic-report-bundle.json";
    private static final String DIAGNOSTIC_REPORT_AFTER_OBSERVATION_BUNDLE =
        FILES_DIRECTORY + "diagnostic-report-after-observation-bundle.json";
    private static final String DIAGNOSTIC_REPORT_PERFORMER_ORGANIZATION =
        FILES_DIRECTORY + "diagnostic-report-performer-organization.json";
    private static final String EXPECTED_DIAGNOSTIC_REPORT_OUTPUT = FILES_DIRECTORY + "expected-diagnostic-report-output.xml";
    private static final String DIAGNOSTIC_REPORT_AGENT_PERSON_XML = FILES_DIRECTORY + "diagnostic-report-agent-person-stub.xml";
    private static final String DIAGNOSTIC_REPORT_AGENT_PERSON_BUNDLE = FILES_DIRECTORY + "diagnostic-report-agent-person-bundle.json";
    private static final String EXPECTED_DIAGNOSTIC_REPORT_AGENT_PERSON_OUTPUT = FILES_DIRECTORY
        + "expected-diagnostic-report-agent-person-output.xml";
    private static final String OBSERVATION_STATEMENT_XML = FILES_DIRECTORY + "observation-statement-stub.xml";
    private static final String CONTAINED_MISCELLANEOUS_RECORDS_BUNDLE = FILES_DIRECTORY + "contained-miscellaneous-records-bundle.json";
    private static final String EXPECTED_MISCELLANEOUS_RECORDS_OUTPUT = FILES_DIRECTORY + "expected-miscellaneous-records-output.xml";
    private static final String CONTAINED_UNKNOWN_RESOURCE_BUNDLE = FILES_DIRECTORY + "contained-unknown-resource-bundle.json";
    private static final String TEST_ID = "b2175be3-29c2-465f-b2c6-323db03c2c7c";

    private NonConsultationResourceMapper nonConsultationResourceMapper;
    private MessageContext messageContext;
    private FhirParseService fhirParseService;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private EncounterComponentsMapper encounterComponentsMapper;

    @BeforeEach
    public void setUp() {
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        fhirParseService = new FhirParseService();
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    public void When_TransformingResourceToEhrComp_Expect_CorrectValuesToBeExtracted(String stubEhrComponentMapperXml, String inputBundle,
        String output) {
        setupMock(ResourceTestFileUtils.getFileContent(stubEhrComponentMapperXml));
        String bundle = ResourceTestFileUtils.getFileContent(inputBundle);
        String expectedOutput = ResourceTestFileUtils.getFileContent(output);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        var translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle).get(0);
        assertThat(translatedOutput).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("endedAllergiesArgs")
    public void When_TransformingEndedAllergyListToEhrComp_Expect_CorrectValuesToBeExtracted(String inputBundle, String output) {
        setupMock(ResourceTestFileUtils.getFileContent(ALLERGY_INTOLERANCE_XML));
        String bundle = ResourceTestFileUtils.getFileContent(inputBundle);
        String expectedOutput = ResourceTestFileUtils.getFileContent(output);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        var translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);
        assertAll(
            () -> assertThat(translatedOutput.size()).isEqualTo(2),
            () -> assertThat(translatedOutput.get(0)).isEqualToIgnoringWhitespace(expectedOutput),
            () -> assertThat(translatedOutput.get(1)).isEqualToIgnoringWhitespace(expectedOutput)
        );
    }

    @Test
    public void When_TransformingContainedResourceToEhrComp_WithSupportedComponent_Expect_CorrectValuesExtracted() {
        setupMock(ResourceTestFileUtils.getFileContent(OBSERVATION_STATEMENT_XML));
        String bundle = ResourceTestFileUtils.getFileContent(CONTAINED_MISCELLANEOUS_RECORDS_BUNDLE);
        String expectedOutput = ResourceTestFileUtils.getFileContent(EXPECTED_MISCELLANEOUS_RECORDS_OUTPUT);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        List<String> translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);

        assertAll(
            () -> assertThat(translatedOutput.size()).isOne(),
            () -> assertThat(translatedOutput.get(0)).isEqualToIgnoringWhitespace(expectedOutput)
        );
    }

    @Test
    public void When_TransformingContainedResourceToEhrComp_WithUnsupportedComponent_Expect_ComponentNotMapped() {
        nonConsultationResourceMapper = new NonConsultationResourceMapper(
            messageContext,
            randomIdGeneratorService,
            encounterComponentsMapper,
            new BloodPressureValidator()
        );

        String unknownMappingStub = "<!-- TestReport/" + TEST_ID + "-->";

        when(encounterComponentsMapper.mapResourceToComponent(any(Resource.class))).thenReturn(Optional.of(unknownMappingStub));

        String bundle = ResourceTestFileUtils.getFileContent(CONTAINED_UNKNOWN_RESOURCE_BUNDLE);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        List<String> translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);

        assertThat(translatedOutput).isEmpty();

    }

    @Test
    public void When_TransformingResourceToEhrComp_Expect_IgnoredResourceToBeIgnored() {
        setupMock(ResourceTestFileUtils.getFileContent("")); // empty filePath provided as this isn't expected to return anything
        String bundle = ResourceTestFileUtils.getFileContent(UNCATAGORISED_IGNORED_RESOURCE_BUNDLE);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        var translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);
        assertThat(translatedOutput).isEmpty();
    }

    @Test
    public void When_TransformingResourcesToEhrComp_Expect_ObservationToBeProcessedLast() {
        // ARRANGE
        setupMock("<MappedResourceStub/>");
        String bundle = ResourceTestFileUtils.getFileContent(DIAGNOSTIC_REPORT_AFTER_OBSERVATION_BUNDLE);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        // ACT
        nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);

        // ASSERT
        var resourceArgumentCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(encounterComponentsMapper, times(2))
            .mapResourceToComponent(resourceArgumentCaptor.capture());

        var mappedResources = resourceArgumentCaptor.getAllValues();
        var mappedResourceTypesInOrder = mappedResources.stream().map(Resource::getResourceType);
        assertThat(mappedResourceTypesInOrder.collect(Collectors.toList()))
            .isEqualTo(asList(ResourceType.DiagnosticReport, ResourceType.Observation));
    }

    private static Stream<Arguments> testArgs() {
        return Stream.of(
            Arguments.of(UNCATAGORISED_OBSERVATION_XML, UNCATAGORISED_OBSERVATION_BUNDLE, EXPECTED_UNCATAGORISED_OBSERVATION_OUTPUT),
            Arguments.of(COMMENT_OBSERVATION_XML, COMMENT_OBSERVATION_BUNDLE, EXPECTED_COMMENT_OBSERVATION_OUTPUT),
            Arguments.of(ALLERGY_INTOLERANCE_XML, ALLERGY_INTOLERANCE_BUNDLE, EXPECTED_ALLERGY_INTOLERANCE_OUTPUT),
            Arguments.of(BLOOD_PRESSURE_XML, BLOOD_PRESSURE_BUNDLE, EXPECTED_BLOOD_PRESSURE_OUTPUT),
            Arguments.of(IMMUNIZATION_XML, IMMUNIZATION_BUNDLE, EXPECTED_IMMUNIZATION_OUTPUT),
            Arguments.of(REFERRAL_REQUEST_XML, REFERRAL_REQUEST_BUNDLE, EXPECTED_REFERRAL_REQUEST_OUTPUT),
            Arguments.of(MEDICATION_REQUEST_XML, MEDICATION_REQUEST_BUNDLE, EXPECTED_MEDICATION_REQUEST_OUTPUT),
            Arguments.of(CONDITION_XML, CONDITION_BUNDLE, EXPECTED_CONDITION_OUTPUT),
            Arguments.of(PROCEDURE_REQUEST_XML, PROCEDURE_REQUEST_BUNDLE, EXPECTED_PROCEDURE_REQUEST_OUTPUT),
            Arguments.of(DOCUMENT_REFERENCE_XML, DOCUMENT_REFERENCE_BUNDLE, EXPECTED_DOCUMENT_REFERENCE_REQUEST_OUTPUT),
            Arguments.of(DIAGNOSTIC_REPORT_XML, DIAGNOSTIC_REPORT_BUNDLE, EXPECTED_DIAGNOSTIC_REPORT_OUTPUT),
            Arguments.of(
                DIAGNOSTIC_REPORT_AGENT_PERSON_XML, DIAGNOSTIC_REPORT_AGENT_PERSON_BUNDLE, EXPECTED_DIAGNOSTIC_REPORT_AGENT_PERSON_OUTPUT),
            Arguments.of(
                DIAGNOSTIC_REPORT_AGENT_PERSON_XML,
                DIAGNOSTIC_REPORT_PERFORMER_ORGANIZATION,
                EXPECTED_DIAGNOSTIC_REPORT_AGENT_PERSON_OUTPUT)
        );
    }

    private static Stream<Arguments> endedAllergiesArgs() {
        return Stream.of(
            Arguments.of(ENDED_ALLERGY_INTOLERANCE_BUNDLE, EXPECTED_ALLERGY_INTOLERANCE_OUTPUT),
            Arguments.of(MULTIPLE_ENDED_ALLERGY_INTOLERANCES_BUNDLE, EXPECTED_ALLERGY_INTOLERANCE_OUTPUT)
        );
    }

    private void setupMock(String stubEhrComponentMapperXml) {
        lenient().when(encounterComponentsMapper.mapResourceToComponent(any(Resource.class)))
            .thenReturn(Optional.of(stubEhrComponentMapperXml));
        nonConsultationResourceMapper = new NonConsultationResourceMapper(messageContext,
            randomIdGeneratorService,
            encounterComponentsMapper,
            new BloodPressureValidator()
        );
    }
}
