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
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructuredRecordMappingServiceTest {

    public static final int ATTACHMENT_1_SIZE = 123;
    public static final int ATTACHMENT_2_SIZE = 234;
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
    private IdMapper idMapper;

    @InjectMocks
    private StructuredRecordMappingService structuredRecordMappingService;

    @Test
    void When_GettingExternalAttachments_Expect_AllDocumentReferenceResourcesAreMapped() {
        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(idMapper.get(ResourceType.DocumentReference, ID_1)).thenReturn(NEW_DOC_REF_ID_1);
        when(idMapper.getOrNew(ResourceType.DocumentManifest, ID_1)).thenReturn(NEW_DOC_MANIFEST_ID_1);
        when(idMapper.get(ResourceType.DocumentReference, ID_2)).thenReturn(NEW_DOC_REF_ID_2);
        when(idMapper.getOrNew(ResourceType.DocumentManifest, ID_2)).thenReturn(NEW_DOC_MANIFEST_ID_2);

        var bundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new Patient()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new DocumentReference() {{
                    getContentFirstRep().setAttachment(new Attachment()
                        .setTitle("some title")
                        .setSize(ATTACHMENT_1_SIZE)
                        .setContentType("text/plain"));
                }}.setId(ID_1)))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new DocumentReference() {{
                    getContentFirstRep().setAttachment(new Attachment()
                        .setSize(ATTACHMENT_2_SIZE)
                        .setContentType("text/html"));
                }}.setId(ID_2)));

        var actualExternalAttachments = structuredRecordMappingService.getExternalAttachments(bundle);

        var expectedAttachment1 = OutboundMessage.ExternalAttachment.builder()
            .referenceId(NEW_DOC_REF_ID_1)
            .hrefId(NEW_DOC_MANIFEST_ID_1)
            .filename("AbsentAttachment111_new_doc_ref_id.xyz")
            .contentType("text/plain")
            .compressed(false)
            .largeAttachment(false)
            .originalBase64(true)
            .length(ATTACHMENT_1_SIZE)
            .build();
        var expectedAttachment2 = OutboundMessage.ExternalAttachment.builder()
            .referenceId(NEW_DOC_REF_ID_2)
            .hrefId(NEW_DOC_MANIFEST_ID_2)
            .filename("222_new_doc_ref_id_222_new_doc_ref_id.xyz")
            .contentType("text/html")
            .compressed(false)
            .largeAttachment(false)
            .originalBase64(true)
            .length(ATTACHMENT_2_SIZE)
            .build();

        assertThat(actualExternalAttachments.get(0)).usingRecursiveComparison().isEqualTo(expectedAttachment1);
        assertThat(actualExternalAttachments.get(1)).usingRecursiveComparison().isEqualTo(expectedAttachment2);
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
