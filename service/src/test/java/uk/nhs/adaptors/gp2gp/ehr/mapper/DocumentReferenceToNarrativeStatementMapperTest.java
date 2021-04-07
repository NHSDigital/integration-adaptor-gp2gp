package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
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
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentReferenceToNarrativeStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY = "/ehr/mapper/documentreference/";
    private static final String INPUT_JSON_BUNDLE =  DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "input-bundle.json";

    private static final String DOCUMENT_REFERENCE_INPUT_JSON_OPTIONAL_DATA = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-1.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_TYPE_TEXT_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-2.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_TYPE_DISPLAY_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-3.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_AVAILABILITY_TIME_CREATED = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-4.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_AUTHOR_ORGANISATION = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-5.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_CUSTODIAN_AND_ORG_NAME = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-6.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_DESCRIPTION = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-7.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_PRACTICE_SETTING_TEXT_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-8.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_PRACTICE_SETTING_DISPLAY_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-9.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_ATTACHMENT_TITLE = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-10.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_WITH_ATTACHMENT_CONTENT_TYPE = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-11.json";
    private static final String DOCUMENT_REFERENCE_INPUT_JSON_REQUIRED_DATA = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "example-document-reference-resource-12.json";

    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_OPTIONAL_DATA = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-1.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_TYPE_TEXT_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-2.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_TYPE_DISPLAY_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-3.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_AVAILABILITY_TIME_CREATED = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-4.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_AUTHOR_ORGANISATION = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-5.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_CUSTODIAN_ORG_NAME = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-6.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_DESCRIPTION = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-7.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_PRACTICE_SETTING_TEXT_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-8.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_PRACTICE_SETTING_DISPLAY_ONLY = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-9.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_ABSENT_ATTACHMENT_TITLE_AND_REFERENCE = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-10.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_WITH_REFERENCE_CONTENT_TYPE = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-11.xml";
    private static final String DOCUMENT_REFERENCE_OUTPUT_XML_REQUIRED_DATA = DOCUMENT_REFERENCE_TEST_FILE_DIRECTORY + "expected-output-narrative-statement-12.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private CharSequence expectedOutputMessage;
    private DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        final String bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        final Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        documentReferenceToNarrativeStatementMapper = new DocumentReferenceToNarrativeStatementMapper(messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }


    @ParameterizedTest
    @MethodSource("documentReferenceResourceFileParams")
    public void When_MappingDocumentReferenceJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) throws IOException {

        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        final String jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);

        final String outputMessage = documentReferenceToNarrativeStatementMapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    private static Stream<Arguments> documentReferenceResourceFileParams() {
        return Stream.of(
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_OPTIONAL_DATA, DOCUMENT_REFERENCE_OUTPUT_XML_OPTIONAL_DATA),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_TYPE_TEXT_ONLY, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_TYPE_TEXT_ONLY),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_TYPE_DISPLAY_ONLY, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_TYPE_DISPLAY_ONLY),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_AVAILABILITY_TIME_CREATED, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_AVAILABILITY_TIME_CREATED),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_AUTHOR_ORGANISATION, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_AUTHOR_ORGANISATION),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_CUSTODIAN_AND_ORG_NAME, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_CUSTODIAN_ORG_NAME),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_DESCRIPTION, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_DESCRIPTION),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_PRACTICE_SETTING_TEXT_ONLY, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_PRACTICE_SETTING_TEXT_ONLY),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_PRACTICE_SETTING_DISPLAY_ONLY, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_PRACTICE_SETTING_DISPLAY_ONLY),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_ATTACHMENT_TITLE, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_ABSENT_ATTACHMENT_TITLE_AND_REFERENCE),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_WITH_ATTACHMENT_CONTENT_TYPE, DOCUMENT_REFERENCE_OUTPUT_XML_WITH_REFERENCE_CONTENT_TYPE),
            Arguments.of(DOCUMENT_REFERENCE_INPUT_JSON_REQUIRED_DATA, DOCUMENT_REFERENCE_OUTPUT_XML_REQUIRED_DATA)
        );
    }

}
