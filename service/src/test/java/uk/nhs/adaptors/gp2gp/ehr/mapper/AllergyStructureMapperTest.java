package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class AllergyStructureMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/allergy/";
    private static final String INPUT_JSON_BUNDLE = TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-1.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-2.json";
    private static final String INPUT_JSON_WITH_PATIENT_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-3.json";
    private static final String INPUT_JSON_WITH_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-4.json";
    private static final String INPUT_JSON_WITH_DATES = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-5.json";
    private static final String INPUT_JSON_WITH_ONSET_DATE_ONLY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-6.json";
    private static final String INPUT_JSON_WITH_REASON_END_DATE_ONLY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-7.json";
    private static final String INPUT_JSON_WITH_NO_DATES = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-8.json";
    private static final String INPUT_JSON_WITH_ENVIRONMENT_CATEGORY = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-9.json";
    private static final String INPUT_JSON_WITH_MEDICATION_CATEGORY = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-10.json";
    private static final String INPUT_JSON_WITH_REACTION = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-11.json";
    private static final String INPUT_JSON_WITH_NO_CATEGORY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-12.json";
    private static final String INPUT_JSON_WITH_UNSUPPORTED_CATEGORY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-13.json";
    private static final String INPUT_JSON_WITH_NO_ASSERTED_DATE = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-14.json";
    private static final String INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_ONE_NOTE = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-15.json";
    private static final String INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_TWO_NOTES = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-16.json";
    private static final String INPUT_JSON_WITH_NO_RELATION_TO_CONDITION = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-17.json";
    private static final String INPUT_JSON_WITH_DEVICE_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-18.json";
    private static final String INPUT_JSON_WITH_RELATED_PERSON_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-19.json";
    private static final String INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NAME_TEXT = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-20.json";
    private static final String INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NO_NAME = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-21.json";

    private static final String OUTPUT_XML_USES_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-1.xml";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-2.xml";
    private static final String OUTPUT_XML_USES_PATIENT_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-3.xml";
    private static final String OUTPUT_XML_USES_DATES = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-4.xml";
    private static final String OUTPUT_XML_USES_ONSET_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-5.xml";
    private static final String OUTPUT_XML_USES_NULL_FLAVOR_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-6.xml";
    private static final String OUTPUT_XML_USES_ENVIRONMENT_CATEGORY = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-7.xml";
    private static final String OUTPUT_XML_USES_MEDICATION_CATEGORY = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-8.xml";
    private static final String OUTPUT_XML_USES_REACTION = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-9.xml";
    private static final String OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_ONE_NOTE = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-10.xml";
    private static final String OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_TWO_NOTES = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-11.xml";
    private static final String OUTPUT_XML_USES_NO_RELATION_TO_CONDITION = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-12.xml";
    private static final String OUTPUT_XML_USES_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-13.xml";
    private static final String OUTPUT_XML_USES_DEVICE_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-14.xml";
    private static final String OUTPUT_XML_USES_RELATED_PERSON_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-15.xml";
    private static final String OUTPUT_XML_USES_RELATED_PERSON_ASSERTER_NO_NAME = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-16.xml";
    private static final String COMMON_ID = "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private AllergyStructureMapper allergyStructureMapper;
    private MessageContext messageContext;

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS, OUTPUT_XML_USES_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_NO_OPTIONAL_TEXT_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PATIENT_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_PATIENT_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_DATES, OUTPUT_XML_USES_DATES),
            Arguments.of(INPUT_JSON_WITH_ONSET_DATE_ONLY, OUTPUT_XML_USES_ONSET_DATE),
            Arguments.of(INPUT_JSON_WITH_REASON_END_DATE_ONLY, OUTPUT_XML_USES_NULL_FLAVOR_DATE),
            Arguments.of(INPUT_JSON_WITH_NO_DATES, OUTPUT_XML_USES_NULL_FLAVOR_DATE),
            Arguments.of(INPUT_JSON_WITH_ENVIRONMENT_CATEGORY, OUTPUT_XML_USES_ENVIRONMENT_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_MEDICATION_CATEGORY, OUTPUT_XML_USES_MEDICATION_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_REACTION, OUTPUT_XML_USES_REACTION),
            Arguments.of(INPUT_JSON_WITH_REACTION, OUTPUT_XML_USES_REACTION),
            Arguments.of(INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_ONE_NOTE, OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_ONE_NOTE),
            Arguments.of(INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_TWO_NOTES, OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_TWO_NOTES),
            Arguments.of(INPUT_JSON_WITH_NO_RELATION_TO_CONDITION, OUTPUT_XML_USES_NO_RELATION_TO_CONDITION),
            Arguments.of(INPUT_JSON_WITH_DEVICE_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_DEVICE_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RELATED_PERSON_ASSERTER, OUTPUT_XML_USES_RELATED_PERSON_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NAME_TEXT, OUTPUT_XML_USES_RELATED_PERSON_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NO_NAME, OUTPUT_XML_USES_RELATED_PERSON_ASSERTER_NO_NAME)
        );
    }

    private static Stream<Arguments> resourceInvalidFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_NO_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_UNSUPPORTED_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_NO_ASSERTED_DATE)
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingAllergyIntoleranceJson_Expect_AllergyStructureXmlOutput(String inputJson, String outputXml)
        throws IOException {
        CharSequence expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        AllergyIntolerance parsedAllergyIntolerance = new FhirParseService().parseResource(jsonInput, AllergyIntolerance.class);

        String outputMessage = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(parsedAllergyIntolerance);
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        lenient().when(codeableConceptCdMapper.mapToNullFlavorCodeableConcept(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        lenient().when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        List.of(ResourceType.Patient, ResourceType.Device)
            .forEach(resourceType -> messageContext.getIdMapper().getOrNew(resourceType, buildIdType(resourceType, COMMON_ID)));
        List.of(ResourceType.Practitioner, ResourceType.Organization)
            .forEach(resourceType -> messageContext.getAgentDirectory().getAgentId(buildReference(resourceType, COMMON_ID)));
        allergyStructureMapper = new AllergyStructureMapper(messageContext, codeableConceptCdMapper, new ParticipantMapper());
    }

    @ParameterizedTest
    @MethodSource("resourceInvalidFileParams")
    public void When_MappingInvalidAllergyIntoleranceJson_Expect_Exception(String inputJson) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        AllergyIntolerance parsedAllergyIntolerance = new FhirParseService().parseResource(jsonInput, AllergyIntolerance.class);

        assertThrows(EhrMapperException.class, ()
            -> allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(parsedAllergyIntolerance));
    }
}
