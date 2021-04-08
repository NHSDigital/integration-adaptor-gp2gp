package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BaseReference;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters.NarrativeStatementTemplateParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentReferenceToNarrativeStatementMapper {

    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");
    private static final String DEFAULT_ATTACHMENT_CONTENT_TYPE = "text/plain";
    private final MessageContext messageContext;

    public String mapDocumentReferenceToNarrativeStatement(final DocumentReference documentReference) {
        final String narrativeStatementId = messageContext.getIdMapper()
            .getOrNew(ResourceType.DocumentReference, documentReference.getId());

        final NarrativeStatementTemplateParametersBuilder builder = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(narrativeStatementId)
            .availabilityTime(getAvailabilityTime(documentReference));

        final Optional<Attachment> contentAttachment = documentReference.getContent()
            .stream()
            .map(DocumentReference.DocumentReferenceContentComponent::getAttachment)
            .findFirst();

        if (contentAttachment.isPresent() && contentAttachment.get().hasTitle()) {

            final Attachment attachment = contentAttachment.get();
            final String attachmentTitle = attachment.getTitle();
            final String attachmentContentType = attachment.hasContentType()
                ? attachment.getContentType() : DEFAULT_ATTACHMENT_CONTENT_TYPE;

            builder.hasReference(true)
                .comment(getComment(documentReference, attachmentTitle))
                .availabilityTime(getAvailabilityTime(documentReference))
                .referenceTitle(attachmentTitle)
                .referenceContentType(attachmentContentType);
        } else {
            builder.comment(getComment(documentReference, null));
        }

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, builder.build());
    }

    private String getComment(final DocumentReference documentReference, final String attachmentTitle) {
        StringBuilder commentBuilder = new StringBuilder();

        mapType(documentReference, commentBuilder);
        mapAuthorOrg(documentReference, commentBuilder);
        mapCustodianOrg(documentReference, commentBuilder);
        mapDescription(documentReference, commentBuilder);

        Optional.ofNullable(attachmentTitle)
            .ifPresent(title -> commentBuilder.append("Absent Attachment: ").append(title).append(StringUtils.SPACE));

        mapSettings(documentReference, commentBuilder);

        return commentBuilder.toString().trim();
    }

    private void mapSettings(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        final Optional<CodeableConcept> codeableConcept = Optional.ofNullable(documentReference.getContext())
            .map(DocumentReference.DocumentReferenceContextComponent::getPracticeSetting);

        codeableConcept
            .filter(CodeableConcept::hasText)
            .map(CodeableConcept::getText)
            .ifPresentOrElse(text -> commentBuilder.append("Setting: ").append(text).append(StringUtils.SPACE),
                () -> codeableConcept
                    .filter(CodeableConcept::hasCoding)
                    .map(CodeableConcept::getCodingFirstRep)
                    .filter(Coding::hasDisplay)
                    .map(Coding::getDisplay)
                    .ifPresent(display -> commentBuilder.append("Setting: ").append(display).append(StringUtils.SPACE)));
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
        final CodeableConcept codeableConcept = documentReference.getType();
        final Optional<CodeableConcept> type = Optional.of(codeableConcept);

        type.filter(CodeableConcept::hasText)
            .map(CodeableConcept::getText)
            .ifPresentOrElse(text -> commentBuilder.append("Type: ").append(text).append(StringUtils.SPACE),
                () -> type.map(CodeableConcept::getCodingFirstRep)
                    .filter(Coding::hasDisplay)
                    .map(Coding::getDisplay)
                    .ifPresent(display -> commentBuilder.append("Type: ").append(display).append(StringUtils.SPACE)));
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