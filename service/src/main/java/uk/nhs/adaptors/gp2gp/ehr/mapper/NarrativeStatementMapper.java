package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BaseReference;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NarrativeStatementMapper {

    private final MessageContext messageContext;

    private static final Mustache NARRATIVE_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");

    public String mapObservationToNarrativeStatement(Observation observation, boolean isNested) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
            .availabilityTime(getAvailabilityTime(observation))
            .comment(observation.getComment())
            .isNested(isNested)
            .build();

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, narrativeStatementTemplateParameters);
    }

    private String getAvailabilityTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return DateFormatUtil.toHl7Format(observation.getEffectiveDateTimeType());
        } else if (observation.hasEffectivePeriod()) {
            return DateFormatUtil.toHl7Format(observation.getEffectivePeriod().getStartElement());
        } else if (observation.hasIssuedElement()) {
            return toHl7Format(observation.getIssuedElement());
        } else {
            throw new EhrMapperException("Could not map effective date");
        }
    }

    public String mapDocumentReferenceToNarrativeStatement(final DocumentReference documentReference) {
        final String narrativeStatementId = messageContext.getIdMapper().getOrNew(ResourceType.DocumentReference, documentReference.getId());

        final NarrativeStatementTemplateParameters.NarrativeStatementTemplateParametersBuilder builder =
            NarrativeStatementTemplateParameters.builder();

        final Optional<Attachment> attachment = documentReference.getContent().stream()
            .map(DocumentReference.DocumentReferenceContentComponent::getAttachment)
            .findFirst();

        if (attachment.isPresent()) {

            final String attachmentTitle = attachment.filter(Attachment::hasTitle)
                .map(Attachment::getTitle)
                .orElse(null);

            builder.narrativeStatementId(narrativeStatementId)
                .hasReference(true)
                .comment(getComment(documentReference, attachmentTitle))
                .availabilityTime(getAvailabilityTime(documentReference))
                .referenceTitle(attachmentTitle)
                .referenceContentType(attachment.map(Attachment::getContentType).orElse("text/plain"));
        } else {
            builder.narrativeStatementId(narrativeStatementId)
                .comment(getComment(documentReference, null))
                .availabilityTime(getAvailabilityTime(documentReference));
        }

        return TemplateUtils.fillTemplate(NARRATIVE_STATEMENT_TEMPLATE, builder.build());
    }

    private String getComment(final DocumentReference documentReference, String attachmentTitle) {
        StringBuilder commentBuilder = new StringBuilder();

        mapType(documentReference, commentBuilder);
        mapAuthorOrg(documentReference, commentBuilder);
        mapCustodianOrg(documentReference, commentBuilder);
        mapDescription(documentReference, commentBuilder);

        Optional.ofNullable(attachmentTitle)
            .ifPresent(title -> commentBuilder.append("Absent Attachment: ").append(title).append(StringUtils.SPACE));

        mapSettings(documentReference, commentBuilder);

        return commentBuilder.toString();
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
                    .map(CodeableConcept::getCoding)
                    .flatMap(coding -> coding.stream().findFirst())
                    .filter(Coding::hasDisplay)
                    .map(Coding::getDisplay)
                    .map(display -> commentBuilder.append("Setting: ").append(display).append(StringUtils.SPACE)));
    }

    private void mapDescription(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        Optional.ofNullable(documentReference.getDescription())
            .ifPresent(desc -> commentBuilder.append("Description: ").append(desc).append(StringUtils.SPACE));
    }

    private void mapCustodianOrg(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        Optional.ofNullable(documentReference.getCustodian())
            .map(BaseReference::getReferenceElement)
            .filter(iIdType -> iIdType.getResourceType().equals(ResourceType.Organization.name()))
            .ifPresent(iIdType -> messageContext.getInputBundleHolder()
                .getResource(iIdType)
                .map(Organization.class::cast)
                .map(Organization::getName)
                .map(name -> commentBuilder.append("Custodian Org: ").append(name).append(StringUtils.SPACE)));
    }

    private void mapAuthorOrg(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        documentReference.getAuthor().stream()
            .map(BaseReference::getReferenceElement)
            .filter(iIdType -> iIdType.getResourceType().equals(ResourceType.Organization.name()))
            .findFirst()
            .ifPresent(iIdType -> messageContext.getInputBundleHolder()
                .getResource(iIdType)
                .map(Organization.class::cast)
                .map(Organization::getName)
                .map(name -> commentBuilder.append("Author Org: ").append(name).append(StringUtils.SPACE)));
    }

    private void mapType(final DocumentReference documentReference, final StringBuilder commentBuilder) {
        final CodeableConcept type = documentReference.getType();

        Optional.of(type)
            .filter(CodeableConcept::hasText)
            .map(CodeableConcept::getText)
            .ifPresentOrElse(text -> commentBuilder.append("Type: ").append(text).append(StringUtils.SPACE),
                () -> type.getCoding()
                    .stream()
                    .findFirst()
                    .filter(Coding::hasDisplay)
                    .map(Coding::getDisplay)
                    .map(display -> commentBuilder.append("Type: ").append(display).append(StringUtils.SPACE)));
    }

    private String getAvailabilityTime(final DocumentReference documentReference) {
        if (documentReference.hasCreatedElement()) {
            return DateFormatUtil.toHl7Format(documentReference.getCreatedElement());
        } else if (documentReference.hasIndexedElement()) {
            return DateFormatUtil.toHl7Format(documentReference.getIndexedElement());
        } else {
            throw new EhrMapperException("Could not map availability time");
        }
    }

}