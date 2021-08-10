package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;
import java.util.stream.Collectors;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IIdType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
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
    private static final String EXCEPTION_COULD_NOT_RESOLVE_REFERENCE = "Could not resolve %s Reference";
    private static final String CODE_SYSTEM = "2.16.840.1.113883.2.1.4.5.5";

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
                    .orElseThrow(() -> new EhrMapperException("Could not resolve Practitioner Reference"));
        } else if (resourceType.equals(ResourceType.HealthcareService.name())) {
            return messageContext.getInputBundleHolder()
                    .getResource(referenceId)
                    .map(HealthcareService.class::cast)
                    .map(HealthcareService::getName)
                    .map(RECIPIENT_HEALTH_CARE_SERVICE::concat)
                    .orElseThrow(() -> new EhrMapperException("Could not resolve HealthcareService Reference"));
        } else if (resourceType.equals(ResourceType.Organization.name())) {
            return messageContext.getInputBundleHolder()
                    .getResource(referenceId)
                    .map(Organization.class::cast)
                    .map(Organization::getName)
                    .map(RECIPIENT_ORG::concat)
                    .orElseThrow(() -> new EhrMapperException("Could not resolve Organization Reference"));
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
                        .orElseThrow(() -> new EhrMapperException("Could not resolve RelatedPerson Reference"));
            } else if (reference.getResourceType().equals(ResourceType.Practitioner.name())) {
                return messageContext.getInputBundleHolder()
                        .getResource(reference)
                        .map(Practitioner.class::cast)
                        .map(Practitioner::getNameFirstRep)
                        .map(RequestStatementExtractor::extractHumanName)
                        .map(NOTE_AUTHOR_PRACTITIONER::concat)
                        .orElseThrow(() -> new EhrMapperException("Could not resolve Practitioner Reference"));
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
                throw new EhrMapperException("SupportingInfo Reference not of expected Resource Type");
        }
    }

    public static String extractDocumentReference(MessageContext messageContext, Reference reference) {
        DocumentReference documentReference = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(DocumentReference.class::cast)
                .orElseThrow(() -> new EhrMapperException(String.format(EXCEPTION_COULD_NOT_RESOLVE_REFERENCE, reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Document:");

        if (documentReference.hasCreated()) {
            stringBuilder.append(" " + formatDateUsingDayPrecision(documentReference.getCreatedElement()));
        } else {
            stringBuilder.append(" " + formatDateUsingDayPrecision(documentReference.getIndexedElement()));
        }

        if(documentReference.hasType()) {
            if (documentReference.getType().hasText()) {
                stringBuilder.append(" " + documentReference.getType().getText());
            } else if (documentReference.getType().hasCoding()) {
                CodeableConceptMappingUtils.extractTextOrCoding(documentReference.getType()).ifPresent(docType -> {
                    stringBuilder.append(" " + docType);
                });
            }
        }

        stringBuilder.append(" " + documentReference.getDescription());
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractObservation(MessageContext messageContext, Reference reference) {
        Observation observation = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(Observation.class::cast)
                .orElseThrow(() -> new EhrMapperException(String.format(EXCEPTION_COULD_NOT_RESOLVE_REFERENCE, reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Observation:");

        if (observation.hasEffectiveDateTimeType()) {
            stringBuilder.append(" " + formatDateUsingDayPrecision(observation.getEffectiveDateTimeType()));
        } else if (observation.hasEffectivePeriod()) {
            stringBuilder.append(" " + formatDateUsingDayPrecision(observation.getEffectivePeriod().getStartElement()));
        }

        CodeableConceptMappingUtils.extractTextOrCoding(observation.getCode()).ifPresent(code -> {
            stringBuilder.append(" " + code);
        });

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractReferralRequest(MessageContext messageContext, Reference reference) {
        ReferralRequest referralRequest = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(ReferralRequest.class::cast)
                .orElseThrow(() -> new EhrMapperException(String.format(EXCEPTION_COULD_NOT_RESOLVE_REFERENCE, reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Referral:");

        if (referralRequest.hasAuthoredOn()) {
            stringBuilder.append(" " + formatDateUsingDayPrecision(referralRequest.getAuthoredOnElement()));
        }

        if (referralRequest.hasReasonCode()) {
            CodeableConceptMappingUtils.extractTextOrCoding(referralRequest.getReasonCode().get(0)).ifPresent(reasonCode -> {
                stringBuilder.append(" " + reasonCode);
            });
        }

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractDiagnosticReport(MessageContext messageContext, Reference reference) {
        DiagnosticReport diagnosticReport = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(DiagnosticReport.class::cast)
                .orElseThrow(() -> new EhrMapperException(String.format(EXCEPTION_COULD_NOT_RESOLVE_REFERENCE, reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Pathology Report:");

        if (diagnosticReport.hasIssued()) {
            stringBuilder.append(" " + formatDateUsingDayPrecision(diagnosticReport.getIssuedElement()));
        }

        Optional<Identifier> identifier = diagnosticReport
                .getIdentifier()
                .stream()
                .filter(d -> d.getSystem().equals(CODE_SYSTEM))
                .findFirst();

        if (identifier.isPresent()) {
            stringBuilder.append(" " + identifier.get().getValue());
        }

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractMedicationRequest(MessageContext messageContext, Reference reference) {
        MedicationRequest medicationRequest = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(MedicationRequest.class::cast)
                .orElseThrow(() -> new EhrMapperException(String.format(EXCEPTION_COULD_NOT_RESOLVE_REFERENCE, reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Medication:");

        if (medicationRequest.hasDispenseRequest()) {
            if (medicationRequest.getDispenseRequest().getValidityPeriod().hasStart()) {
                stringBuilder.append(" " + formatDateUsingDayPrecision(medicationRequest.getDispenseRequest().getValidityPeriod().getStartElement()));
            }
        }

        if (medicationRequest.hasMedicationReference()) {
            Medication medication = messageContext
                    .getInputBundleHolder()
                    .getResource(medicationRequest.getMedicationReference().getReferenceElement())
                    .map(Medication.class::cast)
                    .orElseThrow(() -> new EhrMapperException(String.format(EXCEPTION_COULD_NOT_RESOLVE_REFERENCE, reference.getReferenceElement().getResourceType())));

            CodeableConceptMappingUtils.extractTextOrCoding(medication.getCode()).ifPresent(code -> {
                stringBuilder.append(" " + code);
            });
        }

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String formatDateUsingDayPrecision(BaseDateTimeType baseDateTimeType) {
        baseDateTimeType.setPrecision(TemporalPrecisionEnum.DAY);
        return DateFormatUtil.toTextFormat(baseDateTimeType);
    }
}
