package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

import java.util.Arrays;
import java.util.List;

import lombok.SneakyThrows;
import org.assertj.core.api.AssertionsForClassTypes;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.SupportedContentTypes;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import wiremock.org.custommonkey.xmlunit.DetailedDiff;
import wiremock.org.custommonkey.xmlunit.XMLUnit;

@ExtendWith(MockitoExtension.class)
class StructuredRecordMappingServiceTest {

    public static final int ATTACHMENT_1_SIZE = 123;
    public static final int ATTACHMENT_2_SIZE = 234;
    private static final int LARGE_MESSAGE_THRESHOLD = 4500000;
    private static final String ID_1 = "111";
    private static final String NEW_DOC_REF_ID_1 = "111_new_doc_ref_id";
    private static final String NEW_DOC_MANIFEST_ID_1 = "111_new_doc_manifest_id";
    private static final String ID_2 = "222";
    private static final String NEW_DOC_REF_ID_2 = "222_new_doc_ref_id";
    private static final String NEW_DOC_MANIFEST_ID_2 = "222_new_doc_manifest_id";
    private static final String TEST_UNSUPPORTED_CONTENTTYPE = "application/text";

    private static final DocumentReference DOCUMENT_REFERENCE_TYPE_1_TEXTPLAIN = buildDocumentReference(
        ID_1, "/" + NEW_DOC_REF_ID_1, null, ATTACHMENT_1_SIZE, "text/plain");
    private static final DocumentReference DOCUMENT_REFERENCE_TYPE_2_TEXTHTML = buildDocumentReference(
        ID_2, "/" + NEW_DOC_REF_ID_2, null, ATTACHMENT_2_SIZE, "text/html");
    private static final DocumentReference DOCUMENT_REFERENCE_TYPE_1_UNSUPPORTED_CONTENTTYPE = buildDocumentReference(
        ID_1, "/" + NEW_DOC_REF_ID_1, null, ATTACHMENT_1_SIZE, TEST_UNSUPPORTED_CONTENTTYPE);
    private static final DocumentReference DOCUMENT_REFERENCE_TYPE_1_NO_URL = buildDocumentReference(
        ID_1, null, "some title", ATTACHMENT_1_SIZE, "text/plain");

    private static final OutboundMessage.ExternalAttachment EXPECTED_ATTACHMENT_PRESENT_1 = buildExternalAttachment(
        NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_1, "/" + NEW_DOC_REF_ID_1, null,
        "111_new_doc_manifest_id.txt", "text/plain", List.of(),
        buildAttachmentDescription(
            "111_new_doc_manifest_id.txt", "text/plain", false,
            false, false, NEW_DOC_MANIFEST_ID_1
        )
    );
    private static final OutboundMessage.ExternalAttachment EXPECTED_ATTACHMENT_PRESENT_2 = buildExternalAttachment(
        NEW_DOC_MANIFEST_ID_2, NEW_DOC_MANIFEST_ID_2, "/" + NEW_DOC_REF_ID_2, null,
        "222_new_doc_manifest_id.html", "text/html", List.of(),
        buildAttachmentDescription(
            "222_new_doc_manifest_id.html", "text/html", false,
            false, false, NEW_DOC_MANIFEST_ID_2
        )
    );
    private static final OutboundMessage.ExternalAttachment EXPECTED_ATTACHMENT_ABSENT_1 = buildExternalAttachment(
        NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_1, null, "some title",
        "AbsentAttachment111_new_doc_manifest_id.txt", "text/plain", List.of(),
        buildAttachmentDescription(
            "AbsentAttachment111_new_doc_manifest_id.txt", "text/plain", false,
            false, false, NEW_DOC_MANIFEST_ID_1
        )
    );
    private static final OutboundMessage.ExternalAttachment EXPECTED_ATTACHMENT_ABSENT_2 = buildExternalAttachment(
        NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_1, "/" + NEW_DOC_REF_ID_1, null,
        "AbsentAttachment111_new_doc_manifest_id.txt", "text/plain", List.of(),
        buildAttachmentDescription(
            "AbsentAttachment111_new_doc_manifest_id.txt", "text/plain", false,
            false, false, NEW_DOC_MANIFEST_ID_1
        )
    );

    @Mock
    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    @Mock
    private EhrExtractMapper ehrExtractMapper;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private MessageContext messageContext;
    @Mock
    private Gp2gpConfiguration gp2gpConfiguration;
    @Mock
    private SupportedContentTypes supportedContentTypes;
    @Mock
    private EhrExtractStatusService ehrExtractStatusService;

    @InjectMocks
    private StructuredRecordMappingService structuredRecordMappingService;

    @BeforeEach
    void setup() {
        lenient().when(messageContext.getIdMapper()).thenReturn(new IdMapper(randomIdGeneratorService));
    }

    @Test
    void When_GettingExternalAttachments_Expect_AllDocumentReferenceResourcesAreMapped() {
        when(randomIdGeneratorService.createNewId()).thenReturn(
            NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_1,
            NEW_DOC_MANIFEST_ID_2, NEW_DOC_MANIFEST_ID_2
        );
        when(gp2gpConfiguration.getLargeAttachmentThreshold()).thenReturn(LARGE_MESSAGE_THRESHOLD);
        when(supportedContentTypes.isContentTypeSupported(any())).thenReturn(true);

        var mappedExternalAttachments = getMappedExternalAttachments(
            DOCUMENT_REFERENCE_TYPE_1_TEXTPLAIN,
            DOCUMENT_REFERENCE_TYPE_2_TEXTHTML
        );

        assertThat(mappedExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(EXPECTED_ATTACHMENT_PRESENT_1);
        assertThat(mappedExternalAttachments.get(1)).usingRecursiveComparison().isEqualTo(EXPECTED_ATTACHMENT_PRESENT_2);
    }

    @Test
    void When_GettingExternalAttachment_WithWrongContentType_Expect_AbsentAttachmentMapped() {
        when(randomIdGeneratorService.createNewId()).thenReturn(NEW_DOC_MANIFEST_ID_1);
        when(gp2gpConfiguration.getLargeAttachmentThreshold()).thenReturn(LARGE_MESSAGE_THRESHOLD);

        var mappedExternalAttachments = getMappedAbsentAttachments(DOCUMENT_REFERENCE_TYPE_1_UNSUPPORTED_CONTENTTYPE);

        assertThat(mappedExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(EXPECTED_ATTACHMENT_ABSENT_2);
    }

    @Test
    public void When_GettingExternalAttachment_WithTitleAndNoUrl_Expect_AbsentAttachmentMapped() {
        when(randomIdGeneratorService.createNewId()).thenReturn(NEW_DOC_MANIFEST_ID_1);
        when(gp2gpConfiguration.getLargeAttachmentThreshold()).thenReturn(LARGE_MESSAGE_THRESHOLD);

        var mappedExternalAttachments = getMappedAbsentAttachments(DOCUMENT_REFERENCE_TYPE_1_NO_URL);

        assertThat(mappedExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(EXPECTED_ATTACHMENT_ABSENT_1);
    }

    @Test
    void When_GettingHL7_Expect_BundleAndStructuredRecordAreMapped() {
        var expectedHL7 = "some hl7";
        var ehrExtractContent = "some ehr extract content";
        var bundle = mock(Bundle.class);
        var structuredTaskDefinition = mock(GetGpcStructuredTaskDefinition.class);
        var ehrExtractTemplateParameters = mock(EhrExtractTemplateParameters.class);

        when(ehrExtractMapper.mapBundleToEhrFhirExtractParams(structuredTaskDefinition, bundle))
            .thenReturn(ehrExtractTemplateParameters);
        when(ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters)).thenReturn(ehrExtractContent);
        when(outputMessageWrapperMapper.map(structuredTaskDefinition, ehrExtractContent))
            .thenReturn(expectedHL7);

        var actualHL7 = structuredRecordMappingService.mapStructuredRecordToEhrExtractXml(structuredTaskDefinition, bundle);

        verify(ehrExtractMapper).mapBundleToEhrFhirExtractParams(structuredTaskDefinition, bundle);
        verify(ehrExtractMapper).mapEhrExtractToXml(ehrExtractTemplateParameters);
        verify(outputMessageWrapperMapper).map(structuredTaskDefinition, ehrExtractContent);

        assertThat(actualHL7).isEqualTo(expectedHL7);
    }

    @Test
    public void When_MapStructuredRecordToXml_Expect_EhrExtractMessageIdIsSaved() {
        var ehrExtractTemplateParameters = mock(EhrExtractTemplateParameters.class);
        var structuredTaskDefinition = mock(GetGpcStructuredTaskDefinition.class);
        var bundle = mock(Bundle.class);
        var randomUUID = "randomUUID";

        when(ehrExtractMapper.mapBundleToEhrFhirExtractParams(any(), any())).thenReturn(ehrExtractTemplateParameters);
        when(ehrExtractTemplateParameters.getEhrExtractId()).thenReturn(randomUUID);
        when(structuredTaskDefinition.getConversationId()).thenReturn(randomUUID);

        structuredRecordMappingService.mapStructuredRecordToEhrExtractXml(structuredTaskDefinition, bundle);

        verify(ehrExtractStatusService).saveEhrExtractMessageId(randomUUID, randomUUID);
    }

    private List<OutboundMessage.ExternalAttachment> getMappedExternalAttachments(DocumentReference... documentReferences) {
        return structuredRecordMappingService.getExternalAttachments(getBundleWith(documentReferences));
    }

    private List<OutboundMessage.ExternalAttachment> getMappedAbsentAttachments(DocumentReference... documentReferences) {
        return structuredRecordMappingService.getAbsentAttachments(getBundleWith(documentReferences));
    }

    private Bundle getBundleWith(DocumentReference... documentReferences) {
        var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(new Patient()));

        Arrays.stream(documentReferences).forEach(
            documentReference -> bundle.addEntry(new Bundle.BundleEntryComponent().setResource(documentReference))
        );
        return bundle;
    }

    @Test
    @SneakyThrows
    public void When_BuildingSkeletonForEhrExtract_Expect_XmlWithSingleComponent() {
        var documentId = "DocumentId";
        var skeletonComponent = "<component />";

        var inputRealEhrExtract = ResourceTestFileUtils
                .getFileContent("/ehr/mapper/ehrExtract/ehrExtract.xml");
        var expectedSkeletonEhrExtract = ResourceTestFileUtils
                .getFileContent("/ehr/mapper/ehrExtract/expectedSkeletonEhrExtract.xml");

        when(ehrExtractMapper.buildEhrCompositionForSkeletonEhrExtract(any())).thenReturn(skeletonComponent);

        var skeletonEhrExtract = structuredRecordMappingService
                .buildSkeletonEhrExtractXml(inputRealEhrExtract, documentId);

        assertXMLEquals(skeletonEhrExtract, expectedSkeletonEhrExtract);
    }

    @Test
    public void When_BuildingSkeletonForEhrExtractWithoutChildComponentNodesToReplace_Expect_XMLWithSingleComponent() throws Exception {
        var documentId = "DocumentId";
        var skeletonComponent = "<component />";

        var inputRealEhrExtract = ResourceTestFileUtils
                .getFileContent("/ehr/mapper/ehrExtract/ehrExtractWithNoComponentsToReplaceForSkeleton.xml");
        var expectedSkeletonEhrExtract = ResourceTestFileUtils
                .getFileContent("/ehr/mapper/ehrExtract/expectedSkeletonEhrExtract.xml");

        when(ehrExtractMapper.buildEhrCompositionForSkeletonEhrExtract(any())).thenReturn(skeletonComponent);

        var skeletonEhrExtract = structuredRecordMappingService
                .buildSkeletonEhrExtractXml(inputRealEhrExtract, documentId);

        assertXMLEquals(skeletonEhrExtract, expectedSkeletonEhrExtract);
    }

    public static void assertXMLEquals(String actualXML, String expectedXML) throws Exception {
        XMLUnit.setIgnoreWhitespace(true);

        var differences = new DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML))
                .getAllDifferences();
        AssertionsForClassTypes.assertThat(differences).isEqualTo(List.of());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static OutboundMessage.ExternalAttachment buildExternalAttachment(String documentID, String messageID, String url, String title,
                                                                              String filename, String contentType,
                                                                              List<Identifier> identifier,
                                                                              OutboundMessage.AttachmentDescription description) {
        return OutboundMessage.ExternalAttachment.builder()
            .title(title)
            .documentId(documentID)
            .messageId(messageID)
            .description(description.toString())
            .url(url)
            .filename(filename)
            .identifier(identifier)
            .contentType(contentType)
            .build();
    }

    private static OutboundMessage.AttachmentDescription buildAttachmentDescription(String fileName, String contentType,
        boolean isCompressed, boolean isLargeAttachment, boolean isOriginalBase64, String documentId) {
        return OutboundMessage.AttachmentDescription.builder()
            .fileName(fileName)
            .contentType(contentType)
            .compressed(isCompressed)
            .largeAttachment(isLargeAttachment)
            .originalBase64(isOriginalBase64)
            .documentId(documentId)
            .build();
    }

    private static DocumentReference buildDocumentReference(String id, String attachmentURL, String attachmentTitle,
        int size, String contentType) {
        var documentReference = new DocumentReference();
        documentReference.setId(buildIdType(ResourceType.DocumentReference, id));
        documentReference.getContentFirstRep().setAttachment(new Attachment()
            .setUrl(attachmentURL)
            .setTitle(attachmentTitle)
            .setSize(size)
            .setContentType(contentType));
        return documentReference;
    }
}
