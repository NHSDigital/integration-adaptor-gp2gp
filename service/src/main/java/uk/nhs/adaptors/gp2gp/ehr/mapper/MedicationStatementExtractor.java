package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BaseExtension;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestIntent;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import com.github.mustachejava.Mustache;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.InFulfilmentOfTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Slf4j
public class MedicationStatementExtractor {
    private static final Mustache IN_FULFILMENT_OF_TEMPLATE =
        TemplateUtils.loadTemplate("in_fulfilment_of_template.mustache");

    private static final String MEDICATION_QUANTITY_TEXT_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationQuantityText-1";
    private static final String PRESCRIPTION_TYPE_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-PrescriptionType-1";
    private static final String REPEAT_INFORMATION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationRepeatInformation-1";
    private static final String NUM_OF_REPEAT_PRESCRIPTIONS_ALLOWED_URL = "numberOfRepeatPrescriptionsAllowed";
    private static final String DEFAULT_REPEAT_VALUE = "1";
    private static final String MEDICATION_STATUS_REASON_STOPPED_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationStatusReason-1";
    private static final String STATUS_REASON_URL = "statusReason";
    private static final String STATUS_CHANGE_URL = "statusChangeDate";
    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_QUANTITY_TEXT = "Unk UoM";
    private static final String NO_INFO_AVAILABLE = "No information available";

    public static String extractDispenseRequestQuantityText(MedicationRequest medicationRequest) {
        return medicationRequest.getDispenseRequest()
            .getExtension()
            .stream()
            .filter(value -> MEDICATION_QUANTITY_TEXT_URL.equals(value.getUrl()))
            .findFirst()
            .map(value -> value.getValue().toString())
            .orElse(DEFAULT_QUANTITY_TEXT);
    }

    public static String extractDispenseRequestQuantityTextFromQuantity(MedicationRequest medicationRequest){
        return medicationRequest.getDispenseRequest()
            .getQuantity()
            .getExtension()
            .stream()
            .filter(value -> MEDICATION_QUANTITY_TEXT_URL.equals(value.getUrl()))
            .findFirst()
            .map(value -> value.getValue().toString())
            .orElse(DEFAULT_QUANTITY_TEXT);
    }

    public static String extractPrescriptionTypeCode(MedicationRequest medicationRequest) {
        return filterExtensionByUrl(medicationRequest, PRESCRIPTION_TYPE_URL)
            .map(Extension::getValue)
            .map(CodeableConcept.class::cast)
            .map(CodeableConcept::getCodingFirstRep)
            .map(Coding::getCode)
            .orElse(StringUtils.EMPTY);
    }

    public static boolean prescriptionTypeTextIsNoInfoAvailable(MedicationRequest medicationRequest) {
        var prescriptionTypeText =  filterExtensionByUrl(medicationRequest, PRESCRIPTION_TYPE_URL)
            .map(Extension::getValue)
            .map(CodeableConcept.class::cast)
            .map(CodeableConcept::getText)
            .orElse(StringUtils.EMPTY);

        return NO_INFO_AVAILABLE.equals(prescriptionTypeText);
    }

    public static String extractRepeatValue(MedicationRequest medicationRequest) {
        var repeatInformation = filterExtensionByUrl(medicationRequest, REPEAT_INFORMATION_URL);

        return repeatInformation.map(Extension::getExtension).stream()
            .flatMap(List::stream)
            .filter(value -> NUM_OF_REPEAT_PRESCRIPTIONS_ALLOWED_URL.equals(value.getUrl()))
            .filter(Extension::hasValue)
            .findFirst()
            .map(BaseExtension::getValueAsPrimitive)
            .map(IPrimitiveType::getValueAsString)
            .orElse(DEFAULT_REPEAT_VALUE);
    }

    public static boolean hasStatusReasonStopped(MedicationRequest medicationRequest) {
        return filterExtensionByUrl(medicationRequest, MEDICATION_STATUS_REASON_STOPPED_URL)
            .isPresent();
    }

    public static String extractStatusReasonStoppedCode(MedicationRequest medicationRequest,
        CodeableConceptCdMapper codeableConceptCdMapper) {
        var statusReason = filterExtensionByUrl(medicationRequest, MEDICATION_STATUS_REASON_STOPPED_URL);

        return statusReason.map(Extension::getExtension).stream()
            .flatMap(List::stream)
            .filter(value -> STATUS_REASON_URL.equals(value.getUrl()))
            .findFirst()
            .map(Extension::getValue)
            .map(CodeableConcept.class::cast)
            .map(codeableConceptCdMapper::mapCodeableConceptToCd)
            .orElse(StringUtils.EMPTY);
    }

    public static String extractStatusReasonStoppedText(MedicationRequest medicationRequest) {
        return filterExtensionByUrl(medicationRequest, MEDICATION_STATUS_REASON_STOPPED_URL)
            .map(Extension::getExtension)
            .stream()
            .flatMap(List::stream)
            .filter(value -> STATUS_REASON_URL.equals(value.getUrl()))
            .findFirst()
            .map(Extension::getValue)
            .map(CodeableConcept.class::cast)
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .map(Optional::get)
            .orElse(StringUtils.EMPTY);
    }

    public static String extractStatusReasonStoppedAvailabilityTime(MedicationRequest medicationRequest) {
        var statusReason = filterExtensionByUrl(medicationRequest, MEDICATION_STATUS_REASON_STOPPED_URL);

        if (statusReason.isEmpty()) {
            return StringUtils.EMPTY;
        }

        return statusReason.get()
            .getExtension()
            .stream()
            .filter(value -> STATUS_CHANGE_URL.equals(value.getUrl()))
            .findFirst()
            .map(Extension::getValue)
            .map(DateTimeType.class::cast)
            .map(DateFormatUtil::toHl7Format)
            .map(value -> String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, value))
            .orElseThrow(() -> new EhrMapperException("Could not resolve Availability Time for Status Reason"));
    }

    public static String extractIdFromPlanMedicationRequestReference(Reference reference, MessageContext messageContext) {
        LOGGER.debug("Ensuring the bundle contains Plan MedicationRequest {}", reference.getReference());

        var resource = messageContext.getInputBundleHolder().getResource(reference.getReferenceElement());

        var medicationRequest = (MedicationRequest) resource.get();
        if (!MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            throw new EhrMapperException("Referenced MedicationRequest does not have plan intent " + reference.getReference());
        }

        return messageContext.getMedicationRequestIdMapper().getOrNew(reference.getReference());
    }

    public static String buildBasedOnCode(String id) {
        var inFulfilmentOfTemplateParameters = InFulfilmentOfTemplateParameters.builder()
            .ehrSupplyAuthoriseId(id)
            .build();

        return TemplateUtils.fillTemplate(IN_FULFILMENT_OF_TEMPLATE, inFulfilmentOfTemplateParameters);
    }
}
