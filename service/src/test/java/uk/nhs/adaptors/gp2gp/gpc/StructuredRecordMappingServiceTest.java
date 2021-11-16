package uk.nhs.adaptors.gp2gp.gpc;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.SupportedContentTypes;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

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

    @Mock
    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    @Mock
    private EhrExtractMapper ehrExtractMapper;
    @Mock
    private MessageContext messageContext;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private Gp2gpConfiguration gp2gpConfiguration;
    @Mock
    private SupportedContentTypes supportedContentTypes;

    private IdMapper idMapper;

    @InjectMocks
    private StructuredRecordMappingService structuredRecordMappingService;

    @Test
    void When_GettingExternalAttachments_Expect_AllDocumentReferenceResourcesAreMapped() {
        when(randomIdGeneratorService.createNewId()).thenReturn(NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_2);
        when(gp2gpConfiguration.getLargeAttachmentThreshold()).thenReturn(LARGE_MESSAGE_THRESHOLD);

        var documentReference1 = buildDocumentReference(ID_1, "/" + NEW_DOC_REF_ID_1,
            "some title", ATTACHMENT_1_SIZE, "text/plain");

        var documentReference2 = buildDocumentReference(ID_2, "/" + NEW_DOC_REF_ID_2,
            null, ATTACHMENT_2_SIZE, "text/html");

        var bundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new Patient()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(documentReference1))
            .addEntry(new Bundle.BundleEntryComponent().setResource(documentReference2));

        var actualExternalAttachments = structuredRecordMappingService.getExternalAttachments(bundle);

        var expectedAttachment1 = buildExternalAttachment(
            NEW_DOC_REF_ID_1, NEW_DOC_MANIFEST_ID_1, "/" + NEW_DOC_REF_ID_1,
            buildAttachmentDescription(
                "AbsentAttachment111_new_doc_ref_id.txt", "text/plain", false,
                false, true, ATTACHMENT_1_SIZE
            )
        );
        var expectedAttachment2 = buildExternalAttachment(
            NEW_DOC_REF_ID_2, NEW_DOC_MANIFEST_ID_2, "/" + NEW_DOC_REF_ID_2,
            buildAttachmentDescription(
                "222_new_doc_ref_id_222_new_doc_ref_id.html", "text/html", false,
                false, true, ATTACHMENT_2_SIZE
            )
        );

        assertThat(actualExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(expectedAttachment1);
        assertThat(actualExternalAttachments.get(1)).usingRecursiveComparison().isEqualTo(expectedAttachment2);
    }

    @Test
    void When_GettingExternalAttachment_WithWrongContentType_Expect_AbsentAttachmentMapped() {
        when(randomIdGeneratorService.createNewId()).thenReturn(NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_2);
        when(gp2gpConfiguration.getLargeAttachmentThreshold()).thenReturn(LARGE_MESSAGE_THRESHOLD);

        var documentReference = buildDocumentReference(ID_1, "/" + NEW_DOC_REF_ID_1,
            "some title", ATTACHMENT_1_SIZE, TEST_UNSUPPORTED_CONTENTTYPE);

        var bundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new Patient()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(documentReference));

        var mappedExternalAttachment = structuredRecordMappingService.getExternalAttachments(bundle).get(0);

        var expectedAttachment = buildExternalAttachment(
            NEW_DOC_REF_ID_1, NEW_DOC_MANIFEST_ID_1, "/" + NEW_DOC_REF_ID_1,
            buildAttachmentDescription(
                "AbsentAttachment111_new_doc_ref_id.txt", "text/plain", false,
                false, true, ATTACHMENT_1_SIZE
            )
        );

        assertThat(mappedExternalAttachment).usingRecursiveComparison().isEqualTo(expectedAttachment);
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

        var actualHL7 = structuredRecordMappingService.getHL7(structuredTaskDefinition, bundle);

        verify(ehrExtractMapper).mapBundleToEhrFhirExtractParams(structuredTaskDefinition, bundle);
        verify(ehrExtractMapper).mapEhrExtractToXml(ehrExtractTemplateParameters);
        verify(outputMessageWrapperMapper).map(structuredTaskDefinition, ehrExtractContent);

        assertThat(actualHL7).isEqualTo(expectedHL7);
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachment(String documentID, String messageID, String URL,
        OutboundMessage.AttachmentDescription description) {
        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentID)
            .messageId(messageID)
            .description(description.toString())
            .url(URL)
            .build();
    }

    private OutboundMessage.AttachmentDescription buildAttachmentDescription(String fileName, String contentType, boolean isCompressed,
        boolean isLargeAttachment, boolean isOriginalBase64, int length) {
        return OutboundMessage.AttachmentDescription.builder()
            .fileName(fileName)
            .contentType(contentType)
            .compressed(isCompressed)
            .largeAttachment(isLargeAttachment)
            .originalBase64(isOriginalBase64)
            .length(length)
            .build();
    }

    private DocumentReference buildDocumentReference(String ID, String attachmentURL, String attachmentTitle, int size, String contentType) {
        var documentReference = new DocumentReference();
        documentReference.setId(buildIdType(ResourceType.DocumentReference, ID));
        documentReference.getContentFirstRep().setAttachment(new Attachment()
            .setUrl(attachmentURL)
            .setTitle(attachmentTitle)
            .setSize(size)
            .setContentType(contentType));
        return documentReference;
    }
}
