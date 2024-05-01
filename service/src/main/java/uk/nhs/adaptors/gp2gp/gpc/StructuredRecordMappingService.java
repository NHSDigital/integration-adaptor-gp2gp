package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.SupportedContentTypes;
import uk.nhs.adaptors.gp2gp.ehr.utils.DocumentReferenceUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class StructuredRecordMappingService {
    private final MessageContext messageContext;
    private final OutputMessageWrapperMapper outputMessageWrapperMapper;
    private final EhrExtractMapper ehrExtractMapper;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final SupportedContentTypes supportedContentTypes;
    private final EhrExtractStatusService ehrExtractStatusService;

    private DocumentBuilder documentBuilder;

    public static final String DEFAULT_ATTACHMENT_CONTENT_TYPE = "text/plain";

    public List<OutboundMessage.ExternalAttachment> getExternalAttachments(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, DocumentReference.class)
                .filter(documentReference -> !qualifiesAsAbsentAttachment(documentReference))
                .map(this::buildExternalAttachment)
                .collect(Collectors.toList());
    }

    public List<OutboundMessage.ExternalAttachment> getAbsentAttachments(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, DocumentReference.class)
                .filter(this::qualifiesAsAbsentAttachment)
                .map(this::buildExternalAttachment)
                .collect(Collectors.toList());
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachment(DocumentReference documentReference) {
        var attachment = DocumentReferenceUtils.extractAttachment(documentReference);
        var documentId = messageContext.getIdMapper()
                .newId(ResourceType.DocumentReference, documentReference.getIdElement());
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
                .description(OutboundMessage.AttachmentDescription.builder()
                        .fileName(fileName)
                        .contentType(contentType)
                        .compressed(false) // always false for GPC documents
                        .largeAttachment(isLargeAttachment(attachment))
                        .originalBase64(false)
                        .documentId(documentId)
                        .build()
                        .toString()
                )
                .url(extractUrl(documentReference).orElse(null))
                .title(documentReference.getContentFirstRep().getAttachment().getTitle())
                .identifier(documentReference.getIdentifier().stream()
                        .map(identifier -> Identifier.builder()
                                .system(identifier.getSystem())
                                .value(identifier.getValue())
                                .build())
                        .collect(Collectors.toList()))
                .filename(fileName)
                .originalDescription(documentReference.getDescription())
                .contentType(contentType)
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

    public String mapStructuredRecordToEhrExtractXml(GetGpcStructuredTaskDefinition structuredTaskDefinition, Bundle bundle) {
        var ehrExtractTemplateParameters = ehrExtractMapper
                .mapBundleToEhrFhirExtractParams(structuredTaskDefinition, bundle);
        String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        ehrExtractStatusService.saveEhrExtractMessageId(structuredTaskDefinition.getConversationId(),
                ehrExtractTemplateParameters.getEhrExtractId());

        return outputMessageWrapperMapper.map(structuredTaskDefinition, ehrExtractContent);
    }

    @SneakyThrows
    public String buildSkeletonEhrExtractXml(String realEhrExtract, String documentIdOfCompressedEhrExtract
    ) {
        var ehrCompositionWithNarrativeStatement =
            ehrExtractMapper.buildEhrCompositionForSkeletonEhrExtract(documentIdOfCompressedEhrExtract);

        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        var document = documentBuilder.parse(new InputSource(new StringReader(realEhrExtract)));
        var ehrFolder = removeComponentsFromEhrFolder(document);
        insertSkeletonEhrCompositionIntoEhrFolder(ehrCompositionWithNarrativeStatement, ehrFolder);

        return toString(document);
    }

    private static String toString(Document document) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    private void insertSkeletonEhrCompositionIntoEhrFolder(String ehrCompositionWithNarrativeStatement, Node ehrFolder)
            throws SAXException, IOException {
        var skeletonNode = documentBuilder.parse(new InputSource(new StringReader(ehrCompositionWithNarrativeStatement)));
        var skeletonNodeDocumentElement = skeletonNode.getDocumentElement();

        ehrFolder.getOwnerDocument().adoptNode(skeletonNodeDocumentElement);
        ehrFolder.appendChild(skeletonNodeDocumentElement);
    }

    private static Node removeComponentsFromEhrFolder(Document document) throws XPathExpressionException {
        var xpathFactory = XPathFactory.newInstance();
        var xpath = xpathFactory.newXPath();
        var xPathExpression = "//EhrExtract//component//ehrFolder//component";
        var parentXPathExpression = "//EhrExtract//component//ehrFolder";

        var componentNodes = (NodeList) xpath.compile(xPathExpression).evaluate(document, XPathConstants.NODESET);
        var parent = (Node) xpath.compile(parentXPathExpression).evaluate(document, XPathConstants.NODE);

        for (var i = 0; i < componentNodes.getLength(); i++) {
            componentNodes.item(i).getParentNode().removeChild(componentNodes.item(i));
        }

        return parent;
    }
}
