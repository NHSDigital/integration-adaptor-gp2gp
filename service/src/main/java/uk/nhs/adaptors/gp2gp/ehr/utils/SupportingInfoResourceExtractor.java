package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;

import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

public class SupportingInfoResourceExtractor {
    private static final String PMIP_CODE_SYSTEM = "2.16.840.1.113883.2.1.4.5.5";
    public static final String URN_OID_PREFIX = "urn:oid:";

    public static String extractDocumentReference(MessageContext messageContext, Reference reference) {
        var documentReferenceOptional = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(DocumentReference.class::cast);

        if (documentReferenceOptional.isEmpty()) {
            return "";
        }

        var stringBuilder = new StringBuilder();
        stringBuilder.append("{ Document:");

        var documentReference = documentReferenceOptional.get();

        if (documentReference.hasCreated()) {
            stringBuilder
                .append(" ")
                .append(formatDateUsingDayPrecision(documentReference.getCreatedElement()));
        } else {
            stringBuilder
                .append(" ")
                .append(formatDateUsingDayPrecision(documentReference.getIndexedElement()));
        }

        CodeableConceptMappingUtils.extractTextOrCoding(documentReference.getType()).ifPresent(
            code -> stringBuilder
                .append(" ")
                .append(code)
        );

        if (documentReference.hasDescription()) {
            stringBuilder
                .append(" ")
                .append(documentReference.getDescription());
        }
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractObservation(MessageContext messageContext, Reference reference) {
        var observationOptional = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(Observation.class::cast);

        if (observationOptional.isEmpty()) {
            return "";
        }

        var stringBuilder = new StringBuilder();
        stringBuilder.append("{ Observation:");

        var observation = observationOptional.get();

        if (observation.hasEffectiveDateTimeType()) {
            stringBuilder
                .append(" ")
                .append(formatDateUsingDayPrecision(observation.getEffectiveDateTimeType()));
        } else if (observation.hasEffectivePeriod()) {
            if (observation.getEffectivePeriod().hasStart()) {
                stringBuilder
                    .append(" ")
                    .append(formatDateUsingDayPrecision(observation.getEffectivePeriod().getStartElement()));
            }
        }

        CodeableConceptMappingUtils.extractTextOrCoding(observation.getCode()).ifPresent(
            code -> stringBuilder
                .append(" ")
                .append(code)
        );

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractReferralRequest(MessageContext messageContext, Reference reference) {
        var referralRequestOptional = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(ReferralRequest.class::cast);

        if (referralRequestOptional.isEmpty()) {
            return "";
        }

        var stringBuilder = new StringBuilder();
        stringBuilder.append("{ Referral:");

        var referralRequest = referralRequestOptional.get();

        if (referralRequest.hasAuthoredOn()) {
            stringBuilder
                .append(" ")
                .append(formatDateUsingDayPrecision(referralRequest.getAuthoredOnElement()));
        }

        if (referralRequest.hasReasonCode()) {
            CodeableConceptMappingUtils.extractTextOrCoding(referralRequest.getReasonCode().get(0)).ifPresent(
                    reasonCode -> stringBuilder
                        .append(" ")
                        .append(reasonCode)
            );
        }
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractDiagnosticReport(MessageContext messageContext, Reference reference) {
        var diagnosticReportOptional = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(DiagnosticReport.class::cast);

        if (diagnosticReportOptional.isEmpty()) {
            return "";
        }

        var stringBuilder = new StringBuilder();
        stringBuilder.append("{ Pathology Report:");

        var diagnosticReport = diagnosticReportOptional.get();

        if (diagnosticReport.hasIssued()) {
            stringBuilder
                .append(" ")
                .append(formatDateUsingDayPrecision(diagnosticReport.getIssuedElement()));
        }

        Optional<Identifier> identifier = diagnosticReport
            .getIdentifier()
            .stream()
            .filter(SupportingInfoResourceExtractor::isPMIPCodeSystem)
            .findFirst();

        identifier.ifPresent(
            value -> stringBuilder
                .append(" ")
                .append(value.getValue())
        );
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static String extractMedicationRequest(MessageContext messageContext, Reference reference) {
        var medicationRequestOptional = messageContext
                .getInputBundleHolder()
                .getResource(reference.getReferenceElement())
                .map(MedicationRequest.class::cast);

        if (medicationRequestOptional.isEmpty()) {
            return "";
        }

        var stringBuilder = new StringBuilder();
        stringBuilder.append("{ Medication:");

        var medicationRequest = medicationRequestOptional.get();

        if (medicationRequest.hasDispenseRequest()) {
            if (medicationRequest.getDispenseRequest().getValidityPeriod().hasStart()) {
                stringBuilder
                    .append(" ")
                    .append(
                        formatDateUsingDayPrecision(
                            medicationRequest.getDispenseRequest().getValidityPeriod().getStartElement()
                        )
                    );
            }
        }

        if (medicationRequest.hasMedicationReference()) {
            messageContext
                .getInputBundleHolder()
                .getResource(medicationRequest.getMedicationReference().getReferenceElement())
                .map(Medication.class::cast)
                .flatMap(medication -> CodeableConceptMappingUtils.extractTextOrCoding(medication.getCode()))
                .ifPresent(code -> stringBuilder
                    .append(" ")
                    .append(code)
                );
        }
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    private static String formatDateUsingDayPrecision(BaseDateTimeType baseDateTimeType) {
        baseDateTimeType.setPrecision(TemporalPrecisionEnum.DAY);
        return DateFormatUtil.toTextFormat(baseDateTimeType);
    }

    private static boolean isPMIPCodeSystem(Identifier identifier) {
        return StringUtils
            .removeStart(identifier.getSystem(), URN_OID_PREFIX)
            .equals(PMIP_CODE_SYSTEM);
    }
}
