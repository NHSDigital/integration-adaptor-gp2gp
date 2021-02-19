package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

public class RequestStatementExtractor {
    private static final String RECIPIENT_PRACTITIONER = "Recipient Practitioner: ";
    private static final String RECIPIENT_HEALTH_CARE_SERVICE = "Recipient Healthcare Service: ";
    private static final String RECIPIENT_ORG = "Recipient Org: ";
    private static final String NOTE_AUTHOR = "Author: ";
    private static final String NOTE_AUTHOR_RELATION = NOTE_AUTHOR + "Relation ";
    private static final String NOTE_AUTHOR_PRACTITIONER = NOTE_AUTHOR + "Practitioner ";
    private static final String NOTE_AUTHOR_PATIENT = NOTE_AUTHOR + "Patient";
    private static final String COMMA = ",";
    private static final String NAME = "%s %s";

    public static String extractServiceRequested(ReferralRequest referralRequest) {
        return referralRequest.getServiceRequested().stream()
            .map(value -> extractTextOrCoding(value))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    public static String extractDevice(Device device) {
        return extractTextOrCoding(device.getType())
            .orElse(StringUtils.EMPTY);
    }

    public static String extractRecipient(MessageContext messageContext, Reference reference) {
        IIdType referenceId = reference.getReferenceElement();
        if (referenceId.getResourceType().equals(ResourceType.Practitioner.name())) {
            return messageContext.getInputBundleHolder()
                .getResource(referenceId)
                .map(resource -> (Practitioner) resource)
                .map(value -> RECIPIENT_PRACTITIONER + extractHumanName(value.getNameFirstRep()))
                .orElse(StringUtils.EMPTY);
        } else if (referenceId.getResourceType().equals(ResourceType.HealthcareService.name())) {
            return messageContext.getInputBundleHolder()
                .getResource(referenceId)
                .map(resource -> (HealthcareService) resource)
                .map(value -> RECIPIENT_HEALTH_CARE_SERVICE + value.getName())
                .orElse(StringUtils.EMPTY);
        } else if (referenceId.getResourceType().equals(ResourceType.Organization.name())) {
            return messageContext.getInputBundleHolder()
                .getResource(referenceId)
                .map(resource -> (Organization) resource)
                .map(value -> RECIPIENT_ORG + value.getName())
                .orElse(StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    public static String extractReasonCode(ReferralRequest referralRequest) {
        var ignoreFirstReasonCode = referralRequest.getReasonCode();
        return ignoreFirstReasonCode.stream()
            .skip(1)
            .map(value -> extractTextOrCoding(value))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    public static String extractAuthor(MessageContext messageContext, Annotation annotation) {
        if (annotation.hasAuthorStringType() && annotation.getAuthorStringType().hasValue()) {
            return NOTE_AUTHOR + annotation.getAuthorStringType().getValue();
        } else if (annotation.hasAuthorReference() && annotation.getAuthorReference().hasReferenceElement()) {
            IIdType reference = annotation.getAuthorReference().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.RelatedPerson.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (RelatedPerson) resource)
                    .map(value -> NOTE_AUTHOR_RELATION + extractHumanName(value.getNameFirstRep()))
                    .orElse(StringUtils.EMPTY);
            } else if (reference.getResourceType().equals(ResourceType.Practitioner.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Practitioner) resource)
                    .map(value -> NOTE_AUTHOR_PRACTITIONER + extractHumanName(value.getNameFirstRep()))
                    .orElse(StringUtils.EMPTY);
            } else if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return NOTE_AUTHOR_PATIENT;
            }
        }

        return StringUtils.EMPTY;
    }

    public static String extractHumanName(HumanName humanName) {
        if  (humanName.hasText()) {
            return humanName.getText();
        } else if (humanName.hasGiven() || humanName.hasFamily()) {
            String given = StringUtils.EMPTY;
            String family = StringUtils.EMPTY;
            if (humanName.getGivenAsSingleString() != null) {
                given = humanName.getGivenAsSingleString();
            }
            if (humanName.getFamily() != null) {
                family = humanName.getFamily();
            }
            return String.format(NAME, given, family);
        }
        return StringUtils.EMPTY;
    }

    public static String extractNoteTime(Annotation annotation) {
        if (annotation.hasTime()) {
            return DateFormatUtil.formatDate(annotation.getTime());
        }
        return StringUtils.EMPTY;
    }
}
