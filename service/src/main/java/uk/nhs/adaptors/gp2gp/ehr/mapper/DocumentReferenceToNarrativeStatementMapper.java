package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BaseReference;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters.NarrativeStatementTemplateParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.DocumentReferenceUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentReferenceToNarrativeStatementMapper {

    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");
    private static final String ATTACHMENT_CONTENT_TYPE_PLACEHOLDER = "CONTENT_TYPE_PLACEHOLDER_ID=";
    private static final String ERROR_MESSAGE_TEMPLATE = "ERROR_MESSAGE_PLACEHOLDER_ID=";
    private static final String FILENAME_TEMPLATE = "FILENAME_PLACEHOLDER_ID=";
    private static final List<String> VALID_AUTHOR_RESOURCE_TYPES = List.of("Organization", "Practitioner");

    private final MessageContext messageContext;
    private final SupportedContentTypes supportedContentTypes;
    private final ParticipantMapper participantMapper;
    private final ConfidentialityService confidentialityService;

    public String mapDocumentReferenceToNarrativeStatement(final DocumentReference documentReference) {
        if (documentReference.getContent().isEmpty()) {
            throw new EhrMapperException("No content found on documentReference");
        }

        final String narrativeStatementId = messageContext.getIdMapper()
            .getOrNew(ResourceType.DocumentReference, documentReference.getIdElement());

        final NarrativeStatementTemplateParametersBuilder builder = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(narrativeStatementId)
            .availabilityTime(getAvailabilityTime(documentReference))
            .confidentialityCode(confidentialityService.generateConfidentialityCode(documentReference).orElse(null))
            .hasReference(true)
            .participant(buildParticipant(documentReference));

        final Attachment attachment = DocumentReferenceUtils.extractAttachment(documentReference);
        final String attachmentContentType = DocumentReferenceUtils.extractContentType(attachment);

        boolean isContentTypeSupported = supportedContentTypes.isContentTypeSupported(attachmentContentType);
        if (!isContentTypeSupported) {
            LOGGER.info("Unsupported ContentType '{}' for DocumentReference ID: {} !",
                        attachmentContentType,
                        documentReference.getIdElement().getIdPart());
        }

        builder.referenceTitle("${" + FILENAME_TEMPLATE + narrativeStatementId + "}")
                .comment(getComment(documentReference, narrativeStatementId))
                .referenceContentType("${" + ATTACHMENT_CONTENT_TYPE_PLACEHOLDER + narrativeStatementId + "}");

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, builder.build());
    }

    private String buildParticipant(DocumentReference documentReference) {
        return Optional.of(documentReference)
            .filter(DocumentReference::hasAuthor)
            .map(DocumentReference::getAuthorFirstRep)
            .filter(this::isValidAuthorReference)
            .map(this::mapAuthorToParticipant)
            .orElse(StringUtils.EMPTY);
    }

    private String mapAuthorToParticipant(Reference reference) {
        return participantMapper.mapToParticipant(messageContext.getAgentDirectory().getAgentId(reference), ParticipantType.PERFORMER);
    }

    private boolean isValidAuthorReference(Reference reference) {
        return VALID_AUTHOR_RESOURCE_TYPES.contains(reference.getReferenceElement().getResourceType());
    }

    private String getComment(final DocumentReference documentReference, final String documentId) {
        StringBuilder commentBuilder = new StringBuilder();

        mapType(documentReference, commentBuilder);
        mapAuthorOrg(documentReference, commentBuilder);
        mapCustodianOrg(documentReference, commentBuilder);
        mapDescription(documentReference, commentBuilder);

        commentBuilder.append("${").append(ERROR_MESSAGE_TEMPLATE).append(documentId).append("}");

        mapSettings(documentReference, commentBuilder);

        return commentBuilder.toString().trim();
    }

    private void mapSettings(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        Optional.ofNullable(documentReference.getContext())
            .map(DocumentReference.DocumentReferenceContextComponent::getPracticeSetting)
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .ifPresent(setting -> commentBuilder.append("Setting: ").append(setting).append(StringUtils.SPACE));
    }

    private void mapDescription(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        Optional.ofNullable(documentReference.getDescription())
            .ifPresent(desc -> commentBuilder.append("Description: ").append(desc).append(StringUtils.SPACE));
    }

    private void mapCustodianOrg(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        Optional.ofNullable(documentReference.getCustodian())
            .map(BaseReference::getReferenceElement)
            .filter(IIdType::hasResourceType)
            .filter(idType -> idType.getResourceType().equals(ResourceType.Organization.name()))
            .ifPresent(idType -> messageContext.getInputBundleHolder()
                .getResource(idType)
                .map(Organization.class::cast)
                .map(Organization::getName)
                .map(name -> commentBuilder.append("Custodian Org: ").append(name).append(StringUtils.SPACE)));
    }

    private void mapAuthorOrg(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        documentReference.getAuthor().stream()
            .map(BaseReference::getReferenceElement)
            .filter(IIdType::hasResourceType)
            .filter(idType -> idType.getResourceType().equals(ResourceType.Organization.name()))
            .findFirst()
            .ifPresent(idType -> messageContext.getInputBundleHolder()
                .getResource(idType)
                .map(Organization.class::cast)
                .map(Organization::getName)
                .map(name -> commentBuilder.append("Author Org: ").append(name).append(StringUtils.SPACE)));
    }

    private void mapType(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        Optional.of(documentReference.getType())
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .ifPresent(type -> commentBuilder.append("Type: ").append(type).append(StringUtils.SPACE));
    }

    private String getAvailabilityTime(final DocumentReference documentReference) {
        return Optional.of(documentReference)
            .filter(DocumentReference::hasCreatedElement)
            .map(DocumentReference::getCreatedElement)
            .map(DateFormatUtil::toHl7Format)
            .orElseGet(() -> Optional.of(documentReference)
                .filter(DocumentReference::hasIndexedElement)
                .map(DocumentReference::getIndexedElement)
                .map(DateFormatUtil::toHl7Format)
                .orElseThrow(() -> new EhrMapperException("Could not map availability time")));
    }
}