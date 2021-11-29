package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.SupportedContentTypes;
import uk.nhs.adaptors.gp2gp.ehr.utils.DocumentReferenceUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class StructuredRecordMappingService {
    private final OutputMessageWrapperMapper outputMessageWrapperMapper;
    private final EhrExtractMapper ehrExtractMapper;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final SupportedContentTypes supportedContentTypes;

    private static final String DEFAULT_ATTACHMENT_CONTENT_TYPE = "text/plain";

    public List<OutboundMessage.ExternalAttachment> getExternalAttachments(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, DocumentReference.class)
            .map(this::buildExternalAttachment)
            .collect(Collectors.toList());
    }

    public List<DocumentReference> getAbsentAttachmentDocumentReferences(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, DocumentReference.class)
            .filter(this::qualifiesAsAbsentAttachment)
            .collect(Collectors.toList());
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachment(DocumentReference documentReference) {
        var attachment = DocumentReferenceUtils.extractAttachment(documentReference);
        var documentId = extractUrl(documentReference).map(GetGpcDocumentTaskDefinition::extractIdFromUrl).orElse(null);
        var messageId = randomIdGeneratorService.createNewId();

        String contentType = DocumentReferenceUtils.extractContentType(attachment);
        String fileName;
        if (supportedContentTypes.isContentTypeSupported(contentType)) {
            fileName = DocumentReferenceUtils.buildPresentAttachmentFileName(documentId, contentType);
        } else {
            fileName = DocumentReferenceUtils.buildMissingAttachmentFileName(documentId);
            contentType = DEFAULT_ATTACHMENT_CONTENT_TYPE;
        }

        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentId)
            .messageId(messageId)
            .filename(fileName) //check if it's allowed to add this field, done for checking equality purposes (a little bit cleaner way)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName(fileName)
                .contentType(contentType)
                .compressed(false) // always false for GPC documents
                .largeAttachment(isLargeAttachment(attachment))
                .originalBase64(true) // always true since GPC gives us a Binary resource which is mandated to have base64 encoded data
                .length(attachment.getSize())
                .build()
                .toString()
            )
            .url(extractUrl(documentReference).orElse(null))
            .build();
    }

    private boolean qualifiesAsAbsentAttachment(DocumentReference documentReference) {
        var attachment = DocumentReferenceUtils.extractAttachment(documentReference);
        String contentType = DocumentReferenceUtils.extractContentType(attachment);

        return !supportedContentTypes.isContentTypeSupported(contentType) || attachment.hasTitle();
    }

    private static Optional<String> extractUrl(DocumentReference documentReference) {
        return documentReference.getContent().stream()
            .map(DocumentReference.DocumentReferenceContentComponent::getAttachment)
            .map(Attachment::getUrl)
            .peek(url -> {
                if (StringUtils.isBlank(url)) {
                    LOGGER.warn("Empty URL on DocumentReference {}", documentReference.getIdElement().getIdPart());
                }
            })
            .filter(StringUtils::isNotBlank)
            .reduce((a, b) -> {
                throw new IllegalStateException(String.format(
                    "There is more than 1 Attachment on DocumentReference %s",
                    documentReference.getIdElement().getIdPart()));
            });
    }

    private boolean isLargeAttachment(Attachment attachment) {
        return attachment.getSize() > gp2gpConfiguration.getLargeAttachmentThreshold();
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

    public String getHL7ForLargeEhrExtract(GetGpcStructuredTaskDefinition structuredTaskDefinition, String bindingDocumentId) {
        return ehrExtractMapper.buildSkeletonEhrExtract(structuredTaskDefinition, bindingDocumentId);
    }
}
