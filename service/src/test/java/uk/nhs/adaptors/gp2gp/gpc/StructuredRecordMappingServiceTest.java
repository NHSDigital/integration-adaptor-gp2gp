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

        DocumentReference documentReference1 = new DocumentReference();
        documentReference1.setId(buildIdType(ResourceType.DocumentReference, ID_1));
        documentReference1.getContentFirstRep().setAttachment(new Attachment()
            .setUrl("/" + NEW_DOC_REF_ID_1)
            .setTitle("some title")
            .setSize(ATTACHMENT_1_SIZE)
            .setContentType("text/plain"));

        DocumentReference documentReference2 = new DocumentReference();
        documentReference2.setId(buildIdType(ResourceType.DocumentReference, ID_2));
        documentReference2.getContentFirstRep().setAttachment(new Attachment()
            .setUrl("/" + NEW_DOC_REF_ID_2)
            .setSize(ATTACHMENT_2_SIZE)
            .setContentType("text/html"));

        var bundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new Patient()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(documentReference1))
            .addEntry(new Bundle.BundleEntryComponent().setResource(documentReference2));

        var actualExternalAttachments = structuredRecordMappingService.getExternalAttachments(bundle);

        var expectedAttachment1 = OutboundMessage.ExternalAttachment.builder()
            .documentId(NEW_DOC_REF_ID_1)
            .messageId(NEW_DOC_MANIFEST_ID_1)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName("AbsentAttachment111_new_doc_ref_id.txt")
                .contentType("text/plain")
                .compressed(false)
                .largeAttachment(false)
                .originalBase64(true)
                .length(ATTACHMENT_1_SIZE)
                .build()
                .toString()
            )
            .url("/" + NEW_DOC_REF_ID_1)
            .build();
        var expectedAttachment2 = OutboundMessage.ExternalAttachment.builder()
            .documentId(NEW_DOC_REF_ID_2)
            .messageId(NEW_DOC_MANIFEST_ID_2)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName("222_new_doc_ref_id_222_new_doc_ref_id.html")
                .contentType("text/html")
                .compressed(false)
                .largeAttachment(false)
                .originalBase64(true)
                .length(ATTACHMENT_2_SIZE)
                .build()
                .toString()
            )
            .url("/" + NEW_DOC_REF_ID_2)
            .build();

        assertThat(actualExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(expectedAttachment1);
        assertThat(actualExternalAttachments.get(1)).usingRecursiveComparison().isEqualTo(expectedAttachment2);
    }

    @Test
    void When_GettingExternalAttachment_WithWrongContentType_Expect_AbsentAttachmentMapped() {
        final String UNSUPPORTED_CONTENTTYPE = "application/text";

        when(randomIdGeneratorService.createNewId()).thenReturn(NEW_DOC_MANIFEST_ID_1, NEW_DOC_MANIFEST_ID_2);
        when(gp2gpConfiguration.getLargeAttachmentThreshold()).thenReturn(LARGE_MESSAGE_THRESHOLD);
        when(supportedContentTypes.isContentTypeSupported(UNSUPPORTED_CONTENTTYPE)).thenReturn(false);

        DocumentReference documentReference1 = new DocumentReference();
        documentReference1.setId(buildIdType(ResourceType.DocumentReference, ID_1));
        documentReference1.getContentFirstRep().setAttachment(new Attachment()
            .setUrl("/" + NEW_DOC_REF_ID_1)
            .setTitle("some title")
            .setSize(ATTACHMENT_1_SIZE)
            .setContentType(UNSUPPORTED_CONTENTTYPE));

        var bundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new Patient()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(documentReference1));

        var actualExternalAttachments = structuredRecordMappingService.getExternalAttachments(bundle);

        var expectedAttachment = OutboundMessage.ExternalAttachment.builder()
            .documentId(NEW_DOC_REF_ID_1)
            .messageId(NEW_DOC_MANIFEST_ID_1)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName("AbsentAttachment111_new_doc_ref_id.txt")
                .contentType("text/plain")
                .compressed(false)
                .largeAttachment(false)
                .originalBase64(true)
                .length(ATTACHMENT_1_SIZE)
                .build()
                .toString()
            )
            .url("/" + NEW_DOC_REF_ID_1)
            .build();

        assertThat(actualExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(expectedAttachment);
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
}
