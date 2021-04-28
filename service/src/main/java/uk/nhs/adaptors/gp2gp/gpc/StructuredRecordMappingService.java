package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.utils.DocumentReferenceUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class StructuredRecordMappingService {
    private final OutputMessageWrapperMapper outputMessageWrapperMapper;
    private final EhrExtractMapper ehrExtractMapper;
    private final MessageContext messageContext;

    public List<OutboundMessage.ExternalAttachment> getExternalAttachments(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, DocumentReference.class)
            .map(this::buildExternalAttachment)
            .collect(Collectors.toList());
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachment(DocumentReference documentReference) {
        var attachment = DocumentReferenceUtils.extractAttachment(documentReference);
        var referenceId = messageContext.getIdMapper().get(ResourceType.DocumentReference, documentReference.getId());
        var manifestReferenceId = messageContext.getIdMapper().getOrNew(ResourceType.DocumentManifest, documentReference.getId());

        return OutboundMessage.ExternalAttachment.builder()
            .referenceId(referenceId)
            .hrefId(manifestReferenceId)
            .contentType(DocumentReferenceUtils.extractContentType(attachment))
            .filename(DocumentReferenceUtils.buildAttachmentFileName(referenceId, attachment))
            .length(attachment.getSize())
            .compressed(false) // always false for GPC documents
            .largeAttachment(false) //TODO: true only if we split >5MB files into chunks https://gpitbjss.atlassian.net/browse/NIAD-1059
            .originalBase64(true) // always true since GPC gives us a Binary resource which is mandated to have base64 encoded data
            .build();
    }

    public String getHL7(GetGpcStructuredTaskDefinition structuredTaskDefinition, Bundle bundle) {
        var ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            structuredTaskDefinition,
            bundle);
        String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        return outputMessageWrapperMapper.map(
            structuredTaskDefinition,
            ehrExtractContent);
    }
}
