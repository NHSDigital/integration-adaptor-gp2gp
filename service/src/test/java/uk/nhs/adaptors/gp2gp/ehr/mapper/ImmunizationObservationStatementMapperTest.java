package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Immunization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;
import static uk.nhs.adaptors.gp2gp.utils.XmlAssertion.assertThatXml;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ImmunizationObservationStatementMapperTest {
    private static final String TEST_ID = "test-id";
    private static final String IMMUNIZATION_FILE_LOCATIONS = "/ehr/mapper/immunization/";
    private static final String OBSERVATION_STATEMENT_CONFIDENTIALITY_CODE_XPATH =
        "/component/ObservationStatement/" + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();

    private static final String INPUT_JSON_WITH_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-all-pertinent-information.json";
    private static final String INPUT_JSON_WITHOUT_REQUIRED_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-pertinent-information.json";
    private static final String INPUT_JSON_WITHOUT_CODEABLE_CONCEPT_TEXT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-codeable-concepts-text.json";
    private static final String INPUT_JSON_WITHOUT_DATE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-date.json";
    private static final String INPUT_JSON_REASON_NOT_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-reason-not-given-coding.json";
    private static final String INPUT_JSON_REASON_NOT_GIVEN_TEXT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-reason-not-given-text.json";
    private static final String INPUT_JSON_WITH_VACCINE_CODE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-vaccine-code-given.json";
    private static final String INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_ONE_NOTE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-with-relation-to-condition-with-one-note.json";
    private static final String INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_TWO_NOTES = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-with-relation-to-condition-with-two-notes.json";
    private static final String INPUT_JSON_WITH_NO_RELATION_TO_CONDITION = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-with-no-relation-to-condition.json";
    private static final String INPUT_JSON_WITHOUT_PRACTITIONER = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-practitioner.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_BUT_NO_ACTOR = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-practitioner-but-no-actor.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_INVALID_REFERENCE_RESOURCE_TYPE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-with-practitioner-invalid-reference-resource-type.json";
    private static final String INPUT_JSON_WITH_REPORT_ORIGIN = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-with-report-origin.json";
    private static final String INPUT_JSON_WITH_PARENT_PRESENT_FALSE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-extension-parent-present-false.json";
    private static final String INPUT_JSON_WITH_NO_PARENT_PRESENT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-extension-no-parent-present.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_REASON_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-multiple-reason-given.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_REASON_NOT_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-multiple-reason-not-given.json";
    private static final String INPUT_JSON_WITH_DOSE_QUANTITY_AS_UNIT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-dose-quantity-as-unit.json";
    private static final String INPUT_JSON_WITH_DOSE_QUANTITY_AS_CODE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-dose-quantity-as-code.json";
    private static final String INPUT_JSON_WITH_DOSE_QUANTITY_NO_UNIT_NO_CODE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-dose-quantity-no-unit-no-code.json";
    private static final String INPUT_JSON_WITH_DOSE_QUANTITY_AS_UNIT_NO_VALUE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-dose-quantity-as-unit-no-value.json";
    private static final String INPUT_JSON_WITH_SITE_USER_SELECTED = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-site-with-user-selected.json";
    private static final String INPUT_JSON_WITH_SITE_NO_USER_SELECTED = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-site-with-no-user-selected.json";
    private static final String INPUT_JSON_WITH_NO_PRIMARY_SOURCE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-primary-source.json";
    private static final String INPUT_JSON_WITH_PRIMARY_SOURCE_FALSE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-primary-source-false.json";
    private static final String INPUT_JSON_WITH_SITE_TERM_TEXT_DISPLAY_SAME = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-site-term-text-display-same.json";
    private static final String INPUT_JSON_WITH_SITE_TERM_TEXT_DISPLAY_DIFFERENT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-site-term-text-display-different.json";
    private static final String INPUT_JSON_WITH_SITE_TERM_TEXT_NO_DISPLAY = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-site-term-text-no-display.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_PRACTITIONERS = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-multiple-practitioners.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_PRACTITIONERS_NO_AP_ROLE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-multiple-practitioners-no-AP-roles.json";
    private static final String INPUT_JSON_BUNDLE = IMMUNIZATION_FILE_LOCATIONS + "fhir-bundle.json";

    private static final String OUTPUT_XML_WITH_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-all-information.xml";
    private static final String OUTPUT_XML_WITHOUT_REQUIRED_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-no-information.xml";
    private static final String OUTPUT_XML_WITHOUT_CONTEXT = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-with-context.xml";
    private static final String OUTPUT_XML_WITHOUT_DATE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-no-date.xml";
    private static final String OUTPUT_XML_WITH_REASON_NOT_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-reason-not-given.xml";
    private static final String OUTPUT_XML_WITH_VACCINE_CODE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-with-vaccine-code.xml";
    private static final String OUTPUT_XML_WITHOUT_PARTICIPANT = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-without-participant.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_WITH_ONE_ADDITIONAL_NOTE_FROM_RELATED_CONDITION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-immunization-with-one-additional-note-from-related-condition.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_WITH_TWO_ADDITIONAL_NOTES_FROM_RELATED_CONDITION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-immunization-with-two-additional-notes-from-related-condition.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_WITH_NO_RELATION_TO_CONDITION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-immunization-with-no-relation-to-condition.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_WITH_REPORT_ORIGIN = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-with-report-origin.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_PARENT_PRESENT_FALSE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-all-information-parent-present-false.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_NO_PARENT_PRESENT = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-all-information-no-parent-present.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_MULTIPLE_REASON_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-multiple-reason-given.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_MULTIPLE_REASON_NOT_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-multiple-reason-not-given.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_DOSE_QUANTITY_NO_UNIT_NO_CODE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-dose-quantity-no-unit-no-code.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_DOSE_QUANTITY_AS_UNIT_NO_VALUE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-dose-quantity-no-value-but-unit.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_SITE_USER_SELECTED = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-site-user-selected.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_SITE_NO_USER_SELECTED = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-site-no-user-selected.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_NO_PRIMARY_SOURCE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-no-primary-source.xml";
    private static final String OUTPUT_XML_WITH_IMMUNIZATION_PRIMARY_SOURCE_FALSE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-primary-source-false.xml";
    private static final String OUTPUT_XML_WITH_SITE_USING_TERM_TEXT = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-site-term-text.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_PRACTITIONERS = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-all-multiple-practitioners.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_PRACTITIONERS_NO_AP_ROLE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-all-multiple-practitioners-no-AP-role.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    @Mock
    private ConfidentialityService confidentialityService;

    private MessageContext messageContext;
    private ImmunizationObservationStatementMapper observationStatementMapper;
    private FhirParseService fhirParseService;

    @BeforeEach
    void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        fhirParseService = new FhirParseService();
        messageContext = new MessageContext(randomIdGeneratorService);
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = fhirParseService.parseResource(bundleInput, Bundle.class);
        messageContext.initialize(bundle);
        observationStatementMapper = new ImmunizationObservationStatementMapper(
            messageContext,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            confidentialityService
        );
    }

    @AfterEach
    void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    void When_MappingImmunizationJson_Expect_ObservationStatementXmlOutput(String inputJson, String outputXml,
                                                                                  boolean isNested) {
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);

        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);
        String outputMessage = observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, isNested);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_PERTINENT_INFORMATION, OUTPUT_XML_WITH_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_WITHOUT_CODEABLE_CONCEPT_TEXT, OUTPUT_XML_WITH_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_WITHOUT_DATE, OUTPUT_XML_WITHOUT_DATE, false),
            Arguments.of(INPUT_JSON_WITHOUT_REQUIRED_PERTINENT_INFORMATION, OUTPUT_XML_WITHOUT_REQUIRED_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_REASON_NOT_GIVEN, OUTPUT_XML_WITH_REASON_NOT_GIVEN, false),
            Arguments.of(INPUT_JSON_REASON_NOT_GIVEN_TEXT, OUTPUT_XML_WITH_REASON_NOT_GIVEN, false),
            Arguments.of(INPUT_JSON_WITH_PERTINENT_INFORMATION, OUTPUT_XML_WITHOUT_CONTEXT, true),
            Arguments.of(INPUT_JSON_WITH_VACCINE_CODE, OUTPUT_XML_WITH_VACCINE_CODE, false),
            Arguments.of(INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_ONE_NOTE,
                OUTPUT_XML_WITH_IMMUNIZATION_WITH_ONE_ADDITIONAL_NOTE_FROM_RELATED_CONDITION, false),
            Arguments.of(INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_TWO_NOTES,
                OUTPUT_XML_WITH_IMMUNIZATION_WITH_TWO_ADDITIONAL_NOTES_FROM_RELATED_CONDITION, false),
            Arguments.of(INPUT_JSON_WITH_NO_RELATION_TO_CONDITION,
                OUTPUT_XML_WITH_IMMUNIZATION_WITH_NO_RELATION_TO_CONDITION, false),
            Arguments.of(INPUT_JSON_WITH_VACCINE_CODE, OUTPUT_XML_WITH_VACCINE_CODE, false),
            Arguments.of(INPUT_JSON_WITHOUT_PRACTITIONER, OUTPUT_XML_WITHOUT_PARTICIPANT, false),
            Arguments.of(INPUT_JSON_WITH_PRACTITIONER_BUT_NO_ACTOR, OUTPUT_XML_WITHOUT_PARTICIPANT, false),
            Arguments.of(INPUT_JSON_WITH_REPORT_ORIGIN, OUTPUT_XML_WITH_IMMUNIZATION_WITH_REPORT_ORIGIN, false),
            Arguments.of(INPUT_JSON_WITH_PARENT_PRESENT_FALSE, OUTPUT_XML_WITH_IMMUNIZATION_PARENT_PRESENT_FALSE, false),
            Arguments.of(INPUT_JSON_WITH_NO_PARENT_PRESENT, OUTPUT_XML_WITH_IMMUNIZATION_NO_PARENT_PRESENT, false),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_REASON_GIVEN, OUTPUT_XML_WITH_IMMUNIZATION_MULTIPLE_REASON_GIVEN, false),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_REASON_NOT_GIVEN, OUTPUT_XML_WITH_IMMUNIZATION_MULTIPLE_REASON_NOT_GIVEN, false),
            Arguments.of(INPUT_JSON_WITH_DOSE_QUANTITY_AS_UNIT, OUTPUT_XML_WITH_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_WITH_DOSE_QUANTITY_AS_CODE, OUTPUT_XML_WITH_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_WITH_DOSE_QUANTITY_NO_UNIT_NO_CODE, OUTPUT_XML_WITH_IMMUNIZATION_DOSE_QUANTITY_NO_UNIT_NO_CODE, false),
            Arguments.of(INPUT_JSON_WITH_DOSE_QUANTITY_AS_UNIT_NO_VALUE,
                OUTPUT_XML_WITH_IMMUNIZATION_DOSE_QUANTITY_AS_UNIT_NO_VALUE, false),
            Arguments.of(INPUT_JSON_WITH_SITE_USER_SELECTED, OUTPUT_XML_WITH_IMMUNIZATION_SITE_USER_SELECTED, false),
            Arguments.of(INPUT_JSON_WITH_SITE_NO_USER_SELECTED, OUTPUT_XML_WITH_IMMUNIZATION_SITE_NO_USER_SELECTED, false),
            Arguments.of(INPUT_JSON_WITH_NO_PRIMARY_SOURCE, OUTPUT_XML_WITH_IMMUNIZATION_NO_PRIMARY_SOURCE, false),
            Arguments.of(INPUT_JSON_WITH_PRIMARY_SOURCE_FALSE, OUTPUT_XML_WITH_IMMUNIZATION_PRIMARY_SOURCE_FALSE, false),
            Arguments.of(INPUT_JSON_WITH_SITE_TERM_TEXT_DISPLAY_SAME, OUTPUT_XML_WITH_SITE_USING_TERM_TEXT, false),
            Arguments.of(INPUT_JSON_WITH_SITE_TERM_TEXT_DISPLAY_DIFFERENT, OUTPUT_XML_WITH_SITE_USING_TERM_TEXT, false),
            Arguments.of(INPUT_JSON_WITH_SITE_TERM_TEXT_NO_DISPLAY, OUTPUT_XML_WITH_SITE_USING_TERM_TEXT, false),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_PRACTITIONERS, OUTPUT_XML_WITH_MULTIPLE_PRACTITIONERS, false),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_PRACTITIONERS_NO_AP_ROLE, OUTPUT_XML_WITH_MULTIPLE_PRACTITIONERS_NO_AP_ROLE, false)
        );
    }

    @Test()
    void When_MappingImmunizationWithInvalidPractitionerReferenceType_Expect_Error() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PRACTITIONER_INVALID_REFERENCE_RESOURCE_TYPE);
        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);

        assertThatThrownBy(() -> observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Not supported agent reference: Patient/6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73");
    }

    @Test
    void When_MappingImmunizationWithoutNopatMetaSecurity_Expect_MessageContainsConfidentialityCode() {
        final var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PERTINENT_INFORMATION);
        final var parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);
        when(confidentialityService.generateConfidentialityCode(parsedImmunization))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final var actualMessage = observationStatementMapper.mapImmunizationToObservationStatement(
            parsedImmunization,
            false
        );

        assertThatXml(actualMessage)
            .containsXPath(OBSERVATION_STATEMENT_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_MappingImmunizationWithoutNoNopatMetaSecurity_Expect_MessageDoesNotContainConfidentialityCode() {
        final var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PERTINENT_INFORMATION);
        final var parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);
        when(confidentialityService.generateConfidentialityCode(parsedImmunization))
            .thenReturn(Optional.empty());

        final var actualMessage = observationStatementMapper.mapImmunizationToObservationStatement(
            parsedImmunization,
            false
        );

        assertThatXml(actualMessage)
            .doesNotContainXPath(OBSERVATION_STATEMENT_CONFIDENTIALITY_CODE_XPATH);
    }
}
