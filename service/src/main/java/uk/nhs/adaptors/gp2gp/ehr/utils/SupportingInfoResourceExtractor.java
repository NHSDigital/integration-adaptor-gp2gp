package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

public class SupportingInfoResourceExtractor {
    private static final String EXCEPTION_COULD_NOT_RESOLVE_REFERENCE = "Could not resolve %s Reference";
    private static final String CODE_SYSTEM = "2.16.840.1.113883.2.1.4.5.5";

    public static String extractDocumentReference(MessageContext messageContext, Reference reference) {
        DocumentReference documentReference = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(DocumentReference.class::cast)
                .orElseThrow(() -> new EhrMapperException(
                        String.format(
                                EXCEPTION_COULD_NOT_RESOLVE_REFERENCE,
                                reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Document:");

        if (documentReference.hasCreated()) {
            stringBuilder.append(" " + formatDateUsingDayPrecision(documentReference.getCreatedElement()));
        } else {
            stringBuilder.append(" " + formatDateUsingDayPrecision(documentReference.getIndexedElement()));
        }

        if (documentReference.hasType()) {
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
                .orElseThrow(() -> new EhrMapperException(
                        String.format(
                                EXCEPTION_COULD_NOT_RESOLVE_REFERENCE,
                                reference.getReferenceElement().getResourceType())));

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
                .orElseThrow(() -> new EhrMapperException(
                        String.format(
                                EXCEPTION_COULD_NOT_RESOLVE_REFERENCE,
                                reference.getReferenceElement().getResourceType())));

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
                .orElseThrow(() -> new EhrMapperException(
                        String.format(
                                EXCEPTION_COULD_NOT_RESOLVE_REFERENCE,
                                reference.getReferenceElement().getResourceType())));

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
                .orElseThrow(() -> new EhrMapperException(
                        String.format(
                                EXCEPTION_COULD_NOT_RESOLVE_REFERENCE,
                                reference.getReferenceElement().getResourceType())));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Medication:");

        if (medicationRequest.hasDispenseRequest()) {
            if (medicationRequest.getDispenseRequest().getValidityPeriod().hasStart()) {
                stringBuilder.append(" "
                        + formatDateUsingDayPrecision(
                        medicationRequest
                                .getDispenseRequest()
                                .getValidityPeriod()
                                .getStartElement()));
            }
        }

        if (medicationRequest.hasMedicationReference()) {
            Medication medication = messageContext
                    .getInputBundleHolder()
                    .getResource(medicationRequest.getMedicationReference().getReferenceElement())
                    .map(Medication.class::cast)
                    .orElseThrow(() -> new EhrMapperException(
                            String.format(
                                    EXCEPTION_COULD_NOT_RESOLVE_REFERENCE,
                                    reference.getReferenceElement().getResourceType())));

            CodeableConceptMappingUtils.extractTextOrCoding(medication.getCode()).ifPresent(code -> {
                stringBuilder.append(" " + code);
            });
        }

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    private static String formatDateUsingDayPrecision(BaseDateTimeType baseDateTimeType) {
        baseDateTimeType.setPrecision(TemporalPrecisionEnum.DAY);
        return DateFormatUtil.toTextFormat(baseDateTimeType);
    }
}
