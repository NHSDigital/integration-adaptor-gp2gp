package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.XmlAssertion.assertThatXml;

@ExtendWith(MockitoExtension.class)
public class DocumentReferenceToNarrativeStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/documentreference/";
    private static final String INPUT_JSON_BUNDLE = TEST_FILE_DIRECTORY + "input-bundle.json";

    private static final String INPUT_JSON_OPTIONAL_DATA = TEST_FILE_DIRECTORY + "example-document-reference-resource-1.json";
    private static final String INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY_AND_SECURITY_LABEL_BUT_WITHOUT_NOPAT
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-with-nopat-security-label-17.json";
    private static final String INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY_LABEL
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-with-nopat-security-label-16.json";
    private static final String INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-with-nopat-security-15.json";
    private static final String INPUT_JSON_WITH_TYPE_TEXT_ONLY = TEST_FILE_DIRECTORY + "example-document-reference-resource-2.json";
    private static final String INPUT_JSON_WITH_TYPE_DISPLAY_ONLY = TEST_FILE_DIRECTORY + "example-document-reference-resource-3.json";
    private static final String INPUT_JSON_WITH_AVAILABILITY_TIME_CREATED
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-4.json";
    private static final String INPUT_JSON_WITH_AUTHOR_ORGANISATION = TEST_FILE_DIRECTORY + "example-document-reference-resource-5.json";
    private static final String INPUT_JSON_WITH_CUSTODIAN_AND_ORG_NAME = TEST_FILE_DIRECTORY + "example-document-reference-resource-6.json";
    private static final String INPUT_JSON_WITH_DESCRIPTION = TEST_FILE_DIRECTORY + "example-document-reference-resource-7.json";
    private static final String INPUT_JSON_WITH_PRACTICE_SETTING_TEXT_ONLY
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-8.json";
    private static final String INPUT_JSON_WITH_PRACTICE_SETTING_DISPLAY_ONLY
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-9.json";
    private static final String INPUT_JSON_WITH_ATTACHMENT_TITLE = TEST_FILE_DIRECTORY + "example-document-reference-resource-10.json";
    private static final String INPUT_JSON_REQUIRED_DATA = TEST_FILE_DIRECTORY + "example-document-reference-resource-11.json";
    private static final String INPUT_JSON_WITH_CUSTODIAN_AND_NO_ORG_NAME
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-12.json";
    private static final String INPUT_JSON_WITH_AUTHOR_PRACTITIONER = TEST_FILE_DIRECTORY + "example-document-reference-resource-13.json";
    private static final String INPUT_JSON_WITH_NOT_SUPPORTED_CONTENT_TYPE
        = TEST_FILE_DIRECTORY + "example-document-reference-resource-14.json";

    private static final String OUTPUT_XML_OPTIONAL_DATA = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-1.xml";
    private static final String OUTPUT_XML_WITH_TYPE_TEXT_ONLY = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-2.xml";
    private static final String OUTPUT_XML_WITH_TYPE_DISPLAY_ONLY = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-3.xml";
    private static final String OUTPUT_XML_WITH_AVAILABILITY_TIME_CREATED
        = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-4.xml";
    private static final String OUTPUT_XML_WITH_AUTHOR_ORGANISATION = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-5.xml";
    private static final String OUTPUT_XML_WITH_CUSTODIAN_ORG_NAME = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-6.xml";
    private static final String OUTPUT_XML_WITH_DESCRIPTION = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-7.xml";
    private static final String OUTPUT_XML_WITH_PRACTICE_SETTING_TEXT_ONLY
        = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-8.xml";
    private static final String OUTPUT_XML_WITH_PRACTICE_SETTING_DISPLAY_ONLY
        = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-9.xml";
    private static final String OUTPUT_XML_WITH_ABSENT_ATTACHMENT_TITLE
        = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-10.xml";
    private static final String OUTPUT_XML_REQUIRED_DATA = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-11.xml";
    private static final String OUTPUT_XML_NOT_SUPPORTED_CONTENT_TYPE = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-12.xml";
    public static final String NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH =
        "/component/NarrativeStatement/reference/referredToExternalDocument/"
        + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private SupportedContentTypes supportedContentTypes;
    @Mock
    private TimestampService timestampService;

    private ConfidentialityService confidentialityService;

    @Mock
    private RedactionsContext redactionsContext;

    private DocumentReferenceToNarrativeStatementMapper mapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        lenient().when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        final String bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        final Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);

        lenient().when(supportedContentTypes.isContentTypeSupported("text/richtext")).thenReturn(true);
        lenient().when(supportedContentTypes.isContentTypeSupported("application/octet-stream")).thenReturn(false);
        lenient().when(timestampService.now()).thenReturn(Instant.parse("2021-08-18T12:00:00.00Z"));

        confidentialityService = new ConfidentialityService(redactionsContext);

        mapper = new DocumentReferenceToNarrativeStatementMapper(
            messageContext, supportedContentTypes, new ParticipantMapper(), confidentialityService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    void When_DocReferenceMetaSecurityAndSecurityLabelPopulatedWithoutNoPat_Expect_NarrativeStatementPopulatesReferredToExternalDocument() {

        final String jsonInput
            = ResourceTestFileUtils.getFileContent(INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY_AND_SECURITY_LABEL_BUT_WITHOUT_NOPAT);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        when(redactionsContext.isRedactionMessage()).thenReturn(true);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThatXml(outputMessage).doesNotContainXPath(NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_DocReferenceSecurityLabelPopulatedWithNoPat_Expect_NarrativeStatementPopulatesReferredToExternalDocument() {

        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY_LABEL);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        when(redactionsContext.isRedactionMessage()).thenReturn(true);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThatXml(outputMessage).containsXPath(NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_DocReferenceSecurityLabelPopulatedWithNoPatAndNotReduction_Expect_NarrativeStatementReferredToExternalDocIsNotPopulated() {

        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY_LABEL);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        when(redactionsContext.isRedactionMessage()).thenReturn(false);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThatXml(outputMessage).doesNotContainXPath(NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
     void When_DocReferenceJsonPopulatedWithNoPat_Expect_NarrativeStatementPopulatesReferredToExternalDocument() {

        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        when(redactionsContext.isRedactionMessage()).thenReturn(true);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThatXml(outputMessage).containsXPath(NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_DocReferenceJsonPopulatedWithNoPatAndNotReduction_Expect_NarrativeStatementDoesNotPopulateReferredToExternalDoc() {

        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_OPTIONAL_DATA_WITH_NOPAT_WITH_SECURITY);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        when(redactionsContext.isRedactionMessage()).thenReturn(false);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThatXml(outputMessage).doesNotContainXPath(NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH);
    }

    @Test
    void When_DocReferenceJsonNotPopulatedWithNoPat_Expect_NarrativeStatementDoesNotPopulateReferredToExternalDocumentWithNopat() {

        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_OPTIONAL_DATA);
        final DocumentReference parsedDocumentReference = new FhirParseService().parseResource(jsonInput, DocumentReference.class);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThatXml(outputMessage).doesNotContainXPath(NARRATIVE_STATEMENT_REFERENCE_CONFIDENTIALITY_CODE_XPATH);
    }

    @ParameterizedTest
    @MethodSource("documentReferenceResourceFileParams")
    void When_MappingDocReferenceJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {

        final CharSequence expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        final String jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        final DocumentReference parsedDocumentReference =
            new FhirParseService().parseResource(jsonInput, DocumentReference.class);

        final String outputMessage = mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    private static Stream<Arguments> documentReferenceResourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_OPTIONAL_DATA, OUTPUT_XML_OPTIONAL_DATA),
            Arguments.of(INPUT_JSON_WITH_TYPE_TEXT_ONLY, OUTPUT_XML_WITH_TYPE_TEXT_ONLY),
            Arguments.of(INPUT_JSON_WITH_TYPE_DISPLAY_ONLY, OUTPUT_XML_WITH_TYPE_DISPLAY_ONLY),
            Arguments.of(INPUT_JSON_WITH_AVAILABILITY_TIME_CREATED, OUTPUT_XML_WITH_AVAILABILITY_TIME_CREATED),
            Arguments.of(INPUT_JSON_WITH_AUTHOR_ORGANISATION, OUTPUT_XML_WITH_AUTHOR_ORGANISATION),
            Arguments.of(INPUT_JSON_WITH_CUSTODIAN_AND_ORG_NAME, OUTPUT_XML_WITH_CUSTODIAN_ORG_NAME),
            Arguments.of(INPUT_JSON_WITH_DESCRIPTION, OUTPUT_XML_WITH_DESCRIPTION),
            Arguments.of(INPUT_JSON_WITH_PRACTICE_SETTING_TEXT_ONLY, OUTPUT_XML_WITH_PRACTICE_SETTING_TEXT_ONLY),
            Arguments.of(INPUT_JSON_WITH_PRACTICE_SETTING_DISPLAY_ONLY, OUTPUT_XML_WITH_PRACTICE_SETTING_DISPLAY_ONLY),
            Arguments.of(INPUT_JSON_WITH_ATTACHMENT_TITLE, OUTPUT_XML_WITH_ABSENT_ATTACHMENT_TITLE),
            Arguments.of(INPUT_JSON_REQUIRED_DATA, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_WITH_CUSTODIAN_AND_NO_ORG_NAME, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_WITH_AUTHOR_PRACTITIONER, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_WITH_NOT_SUPPORTED_CONTENT_TYPE, OUTPUT_XML_NOT_SUPPORTED_CONTENT_TYPE)
        );
    }

    @Test
    void When_MappingParsedDocumentReferenceJsonWithNoDates_Expect_MapperException() {
        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_REQUIRED_DATA);
        final DocumentReference parsedDocumentReference =
            new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        parsedDocumentReference.setIndexedElement(null);

        assertThatThrownBy(() -> mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Could not map availability time");
    }

    @Test
    void When_MappingParsedDocumentReferenceJsonWithNoContent_Expect_MapperException() {
        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_REQUIRED_DATA);
        final DocumentReference parsedDocumentReference =
            new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        parsedDocumentReference.setContent(null);

        assertThatThrownBy(() -> mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("No content found on documentReference");
    }

    @Test
    void When_MappingParsedDocumentReferenceJsonWithContentAndNoAttachment_Expect_MapperException() {
        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_REQUIRED_DATA);
        final DocumentReference parsedDocumentReference =
            new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        parsedDocumentReference.getContent().getFirst().setAttachment(null);

        assertThatThrownBy(() -> mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("documentReference.content[0] is missing an attachment");
    }

    @Test
    void When_MappingParsedDocumentReferenceJsonWithNoAttachmentContentType_Expect_MapperException() {
        final String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_REQUIRED_DATA);
        final DocumentReference parsedDocumentReference =
            new FhirParseService().parseResource(jsonInput, DocumentReference.class);
        final Attachment attachment = parsedDocumentReference.getContent().getFirst().getAttachment();
        attachment.setTitle("some title").setContentType(null);

        assertThatThrownBy(() -> mapper.mapDocumentReferenceToNarrativeStatement(parsedDocumentReference))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("documentReference.content[0].attachment is missing contentType");
    }
}
