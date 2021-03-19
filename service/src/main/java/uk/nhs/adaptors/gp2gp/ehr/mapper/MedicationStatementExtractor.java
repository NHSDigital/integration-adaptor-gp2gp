package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestIntent;

import com.github.mustachejava.Mustache;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.InFulfilmentOfTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

public class MedicationStatementExtractor {
    private static final Mustache IN_FULFILMENT_OF_TEMPLATE =
        TemplateUtils.loadTemplate("in_fulfilment_of_template.mustache");

    private static final String MEDICATION_QUANTITY_TEXT = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationQuantityText-1";
    private static final String PRESCRIPTION_TYPE_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-PrescriptionType-1";
    private static final String REPEAT_INFORMATION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationRepeatInformation-1";
    private static final String NUM_OF_REPEAT_PRESCRIPTIONS_ALLOWED_URL = "numberOfRepeatPrescriptionsAllowed";
    private static final String DEFAULT_NON_ACUTE_REPEAT_VALUE = "1";
    private static final String MEDICATION_STATUS_REASON_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationStatusReason-1";
    private static final String STATUS_REASON_URL = "statusReason";
    private static final String STATUS_CHANGE_URL = "statusChangeDate";
    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_AVAILABILITY_TIME_VALUE = "<availabilityTime nullFlavor=\"UNK\"/>";

    public static String extractDispenseRequestQuantityText(MedicationRequest medicationRequest) {
        return medicationRequest.getDispenseRequest()
            .getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(MEDICATION_QUANTITY_TEXT))
            .findFirst()
            .map(value -> value.getValue().toString())
            .orElse(StringUtils.EMPTY);
    }

    public static String extractStatusReasonCode(MedicationRequest medicationRequest, CodeableConceptCdMapper codeableConceptCdMapper) {
        var statusReasonCode = medicationRequest.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(MEDICATION_STATUS_REASON_URL))
            .findFirst();

        if (statusReasonCode.isPresent()) {
            return statusReasonCode.get()
                .getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(STATUS_REASON_URL))
                .findFirst()
                .map(value -> codeableConceptCdMapper.mapCodeableConceptToCd((CodeableConcept) value.getValue()))
                .orElse(StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    public static String extractPrescriptionTypeCode(MedicationRequest medicationRequest) {
        return medicationRequest.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(PRESCRIPTION_TYPE_URL))
            .findFirst()
            .map(value -> (CodeableConcept) value.getValue())
            .map(code -> code.getCodingFirstRep().getCode())
            .orElse(StringUtils.EMPTY);
    }

    public static String extractNonAcuteRepeatValue(MedicationRequest medicationRequest) {
        var repeatInformation = medicationRequest.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(REPEAT_INFORMATION_URL))
            .findFirst();

        if (repeatInformation.isPresent()) {
            return repeatInformation.get()
                .getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(NUM_OF_REPEAT_PRESCRIPTIONS_ALLOWED_URL) && value.hasValue())
                .findFirst()
                .map(value -> value.getValueAsPrimitive().getValueAsString()) // TODO: Remove "UnsignedIntType" from value returned
                .orElse(DEFAULT_NON_ACUTE_REPEAT_VALUE);
        }

        return DEFAULT_NON_ACUTE_REPEAT_VALUE;
    }

    public static String extractStatusReasonAvailabilityTime(MedicationRequest medicationRequest) {
        var statusReason = medicationRequest.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(MEDICATION_STATUS_REASON_URL))
            .findFirst();

        if (statusReason.isPresent()) {
            return statusReason.get()
                .getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(STATUS_CHANGE_URL))
                .findFirst()
                .map(value -> (DateTimeType) value.getValue())
                .map(DateFormatUtil::toHl7Format)
                .map(value -> String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, value))
                .orElse(DEFAULT_AVAILABILITY_TIME_VALUE);
        }

        return StringUtils.EMPTY;
    }

    public static String extractBasedOn(Reference reference, MessageContext messageContext) {
        var resource = messageContext.getInputBundleHolder()
            .getResource(reference.getReferenceElement())
            .map(value -> (MedicationRequest) value)
            .filter(value -> value.getIntent().getDisplay().equals(MedicationRequestIntent.PLAN.getDisplay()));

        if (resource.isPresent()) {
            return messageContext.getMedicationStatementIdMapper()
                .getOrNew(reference.getIdElement().getId());
        }

        throw new EhrMapperException("Could not resolve basedOn MedicationRequest Reference");
    }

    public static String buildBasedOnCode(String id) {
        var inFulfilmentOfTemplateParameters = InFulfilmentOfTemplateParameters.builder()
            .ehrSupplyAuthoriseId(id);

        return TemplateUtils.fillTemplate(IN_FULFILMENT_OF_TEMPLATE, inFulfilmentOfTemplateParameters);
    }
}
