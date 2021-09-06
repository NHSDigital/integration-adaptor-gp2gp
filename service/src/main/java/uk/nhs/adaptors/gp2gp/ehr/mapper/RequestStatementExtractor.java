package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

import static uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor.extractDocumentReference;
import static uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor.extractObservation;
import static uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor.extractReferralRequest;
import static uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor.extractDiagnosticReport;
import static uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor.extractMedicationRequest;

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
    private static final String EMPTY_STRING = "";

    public static String extractServiceRequested(ReferralRequest referralRequest) {
        return referralRequest.getServiceRequested().stream()
                .map(CodeableConceptMappingUtils::extractTextOrCoding)
                .flatMap(Optional::stream)
                .collect(Collectors.joining(COMMA));
    }

    public static String extractRecipient(MessageContext messageContext, Reference reference) {
        IIdType referenceId = reference.getReferenceElement();
        String resourceType = referenceId.getResourceType();
        if (resourceType.equals(ResourceType.Practitioner.name())) {
            return messageContext.getInputBundleHolder()
                    .getResource(referenceId)
                    .map(Practitioner.class::cast)
                    .map(Practitioner::getNameFirstRep)
                    .map(RequestStatementExtractor::extractHumanName)
                    .map(RECIPIENT_PRACTITIONER::concat)
                    .get();
        } else if (resourceType.equals(ResourceType.HealthcareService.name())) {
            return messageContext.getInputBundleHolder()
                    .getResource(referenceId)
                    .map(HealthcareService.class::cast)
                    .map(HealthcareService::getName)
                    .map(RECIPIENT_HEALTH_CARE_SERVICE::concat)
                    .get();
        } else if (resourceType.equals(ResourceType.Organization.name())) {
            return messageContext.getInputBundleHolder()
                    .getResource(referenceId)
                    .map(Organization.class::cast)
                    .map(Organization::getName)
                    .map(RECIPIENT_ORG::concat)
                    .get();
        }
        throw new EhrMapperException("Recipient Reference not of expected Resource Type");
    }

    public static String extractReasonCode(ReferralRequest referralRequest) {
        var ignoreFirstReasonCode = referralRequest.getReasonCode();
        return ignoreFirstReasonCode.stream()
                .skip(1)
                .map(CodeableConceptMappingUtils::extractTextOrCoding)
                .flatMap(Optional::stream)
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
                        .map(RelatedPerson.class::cast)
                        .map(RelatedPerson::getNameFirstRep)
                        .map(RequestStatementExtractor::extractHumanName)
                        .map(NOTE_AUTHOR_RELATION::concat)
                        .get();
            } else if (reference.getResourceType().equals(ResourceType.Practitioner.name())) {
                return messageContext.getInputBundleHolder()
                        .getResource(reference)
                        .map(Practitioner.class::cast)
                        .map(Practitioner::getNameFirstRep)
                        .map(RequestStatementExtractor::extractHumanName)
                        .map(NOTE_AUTHOR_PRACTITIONER::concat)
                        .get();
            } else if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return NOTE_AUTHOR_PATIENT;
            }
            throw new EhrMapperException("Author Reference not of expected Resource Type");
        }

        return StringUtils.EMPTY;
    }

    public static String extractHumanName(HumanName humanName) {
        if (humanName.hasText()) {
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
            return DateFormatUtil.toTextFormat(annotation.getTimeElement());
        }
        return StringUtils.EMPTY;
    }

    public static String extractSupportingInfo(MessageContext messageContext, Reference reference) {
        IIdType referenceId = reference.getReferenceElement();
        String resourceType = referenceId.getResourceType();

        switch (resourceType) {
            case "DocumentReference":
                return extractDocumentReference(messageContext, reference);
            case "Observation":
                return extractObservation(messageContext, reference);
            case "MedicationRequest":
                return extractMedicationRequest(messageContext, reference);
            case "ReferralRequest":
                return extractReferralRequest(messageContext, reference);
            case "DiagnosticReport":
                return extractDiagnosticReport(messageContext, reference);
            default:
                return EMPTY_STRING;
        }
    }
}
