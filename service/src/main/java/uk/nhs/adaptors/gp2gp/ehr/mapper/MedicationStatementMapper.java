package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractNonAcuteRepeatValue;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractPrescriptionTypeCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestIntent;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestStatus;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.InFulfilmentOfTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.MedicationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class MedicationStatementMapper {
    private static final Mustache MEDICATION_STATEMENT_AUTHORISE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_medication_statement_authorise_template.mustache");
    private static final Mustache MEDICATION_STATEMENT_PRESCRIBE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_medication_statement_prescribe_template.mustache");
    private static final Mustache IN_FULFILMENT_OF_TEMPLATE =
        TemplateUtils.loadTemplate("in_fulfilment_of_template.mustache");

    private static final String ACTIVE_STATUS_CODE = "ACTIVE";
    private static final String COMPLETE_STATUS_CODE = "COMPLETE";
    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_AVAILABILITY_TIME_VALUE = "<availabilityTime nullFlavor=\"UNK\"/>";
    private static final String NOTES = "Notes: %s";
    private static final String EXPECTED_SUPPLY_DURATION = "Expected Supply Duration: %s %s";
    private static final String PATIENT_INSTRUCTION = "Patient Instruction: %s";
    private static final String DEFAULT_QUANTITY_VALUE = "1";
    private static final String DEFAULT_QUANTITY_TEXT = "Unk UoM";
    private static final String MEDICATION_STATUS_REASON_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-MedicationStatusReason-1";
    private static final String STATUS_REASON_URL = "statusReason";
    private static final String STATUS_CHANGE_URL = "statusChangeDate";
    private static final List<String> ACUTE_PRESCRIPTION_TYPE_CODES = Arrays.asList("acute", "acute-handwritten");
    private static final List<String> NON_ACUTE_PRESCRIPTION_TYPE_CODES = Arrays.asList("delayed-prescribing", "repeat", "repeat-dispensing");
    private static final String ACUTE_REPEAT_VALUE = "0";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapMedicationRequestToMedicationStatement(MedicationRequest medicationRequest) {
        var medicationStatementTemplateParameters = MedicationStatementTemplateParameters.builder()
            .medicationStatementId(messageContext.getIdMapper().getOrNew(ResourceType.MedicationRequest, medicationRequest.getId()))
            .statusCode(buildStatusCode(medicationRequest))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForMedicationRequest(medicationRequest))
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForMedicationRequest(medicationRequest))
            .medicationReferenceCode(buildMedicationReferenceCode(medicationRequest))
            .ehrSupplyId(messageContext.getMedicationStatementReferenceIdMapper().getOrNew(medicationRequest.getId()))
            .medicationStatementPertinentInformation(buildDosageInstructionPertinentInformation(medicationRequest))
            .ehrSupplyPertinentInformation(buildPertinentInformation(medicationRequest))
            .repeatNumber(buildRepeatValue(medicationRequest))
            .quantityValue(buildQuantityValue(medicationRequest))
            .quantityText(buildQuantityText(medicationRequest))
            .ehrSupplyDiscontinueCode(buildStatusReasonCode(medicationRequest))
            .ehrSupplyDiscontinueId(randomIdGeneratorService.createNewId())
            .ehrSupplyDiscontinueAvailabilityTime(buildStatusReasonAvailabilityTime(medicationRequest))
            .hasPriorPrescription(medicationRequest.hasPriorPrescription())
            .basedOn(buildBasedOn(medicationRequest))
            .build();

        if (medicationRequest.getIntent().getDisplay().equals(MedicationRequestIntent.PLAN.getDisplay())) {
            return TemplateUtils.fillTemplate(MEDICATION_STATEMENT_AUTHORISE_TEMPLATE, medicationStatementTemplateParameters);
        } else if (medicationRequest.getIntent().getDisplay().equals(MedicationRequestIntent.ORDER.getDisplay())) {
            return TemplateUtils.fillTemplate(MEDICATION_STATEMENT_PRESCRIBE_TEMPLATE, medicationStatementTemplateParameters);
        }

        throw new EhrMapperException("Could not map Medication Request intent");
    }

    private String buildStatusCode(MedicationRequest medicationRequest) {
        if (medicationRequest.getStatus().getDisplay().equals(MedicationRequestStatus.ACTIVE.getDisplay())) {
            return ACTIVE_STATUS_CODE;
        }
        return COMPLETE_STATUS_CODE;
    }

    private String buildMedicationReferenceCode(MedicationRequest medicationRequest) {
        IIdType reference = medicationRequest.getMedicationReference().getReferenceElement();
        return messageContext.getInputBundleHolder()
            .getResource(reference)
            .map(resource -> (Medication) resource)
            .map(value -> codeableConceptCdMapper.mapCodeableConceptToCd(value.getCode()))
            .orElseThrow(() -> new EhrMapperException("Could not resolve Medication Reference"));
    }

    private String buildDosageInstructionPertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDosageInstruction() && medicationRequest.getDosageInstructionFirstRep().hasText()) {
            return medicationRequest.getDosageInstructionFirstRep().getText();
        }
        return StringUtils.EMPTY;
    }

    private String buildPertinentInformation(MedicationRequest medicationRequest) {
        List<String> descriptionList = retrievePertinentInformation(medicationRequest);
        return descriptionList
            .stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrievePertinentInformation(MedicationRequest medicationRequest) {
        return List.of(
            buildPatientInstructionPertinentInformation(medicationRequest),
            buildExpectedSupplyDurationPertinentInformation(medicationRequest),
            buildNotePertinentInformation(medicationRequest)
        );
    }

    private String buildQuantityValue(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasQuantity()) {
            return medicationRequest.getDispenseRequest().getQuantity().getValue().toString();
        }
        return DEFAULT_QUANTITY_VALUE;
    }

    private String buildQuantityText(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasQuantity()) {
            return medicationRequest.getDispenseRequest().getQuantity().getUnit();
        } else if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().getQuantity().hasExtension()) {
            // TODO: use toString?
            return ((StringType) medicationRequest.getDispenseRequest().getQuantity().getExtensionFirstRep().getValue()).getValueAsString();
        }

        return DEFAULT_QUANTITY_TEXT;
    }

    private String buildExpectedSupplyDurationPertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasExpectedSupplyDuration()) {
            return String.format(EXPECTED_SUPPLY_DURATION,
                medicationRequest.getDispenseRequest().getExpectedSupplyDuration().getValue().toString(),
                medicationRequest.getDispenseRequest().getExpectedSupplyDuration().getUnit());
        }
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasNote()) {
            List<Annotation> annotations = medicationRequest.getNote();
            var notes = annotations.stream()
                .map(Annotation::getText)
                .collect(Collectors.joining(StringUtils.SPACE));
            return String.format(NOTES, notes);
        }
        return StringUtils.EMPTY;
    }

    private String buildPatientInstructionPertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDosageInstruction() && medicationRequest.getDosageInstructionFirstRep().hasPatientInstruction()) {
            return String.format(PATIENT_INSTRUCTION, medicationRequest.getDosageInstructionFirstRep().getPatientInstruction());
        }
        return StringUtils.EMPTY;
    }

    private String buildStatusReasonCode(MedicationRequest medicationRequest) {
        if (medicationRequest.getIntent().getDisplay().equals(MedicationRequestIntent.PLAN.getDisplay())) {
            return medicationRequest.getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(MEDICATION_STATUS_REASON_URL))
                .findFirst()
                .get() // TODO: refactor
                .getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(STATUS_REASON_URL))
                .findFirst()
                .map(value -> codeableConceptCdMapper.mapCodeableConceptToCd((CodeableConcept) value.getValue()))
                .orElse(StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    private String buildStatusReasonAvailabilityTime(MedicationRequest medicationRequest) {
        if (medicationRequest.getIntent().getDisplay().equals(MedicationRequestIntent.PLAN.getDisplay())) {
            return medicationRequest.getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(MEDICATION_STATUS_REASON_URL))
                .findFirst()
                .get() // TODO: refactor
                .getExtension()
                .stream()
                .filter(value -> value.getUrl().equals(STATUS_CHANGE_URL))
                .findFirst()
                .map(value -> (DateTimeType) value.getValue())
                .map(DateFormatUtil::toHl7Format)
                .map(value -> String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, value)) // TODO: refactor?
                .orElse(DEFAULT_AVAILABILITY_TIME_VALUE);
        }
        return StringUtils.EMPTY;
    }

    private String buildRepeatValue(MedicationRequest medicationRequest) {
        if (medicationRequest.getIntent().getDisplay().equals(MedicationRequestIntent.PLAN.getDisplay())) {
            var prescriptionTypeCode = extractPrescriptionTypeCode(medicationRequest);

            if (ACUTE_PRESCRIPTION_TYPE_CODES.contains(prescriptionTypeCode)) {
                return ACUTE_REPEAT_VALUE;
            } else if (NON_ACUTE_PRESCRIPTION_TYPE_CODES.contains(prescriptionTypeCode)) {
                return extractNonAcuteRepeatValue(medicationRequest);
            }
            throw new EhrMapperException("Could not match Prescription Type to Repeat value");
        }
        return StringUtils.EMPTY;
    }

    private String buildBasedOn(MedicationRequest medicationRequest) {
        if (medicationRequest.hasBasedOn() && medicationRequest.getIntent().getDisplay().equals(MedicationRequestIntent.ORDER.getDisplay())) {
            return medicationRequest.getBasedOn()
                .stream()
                .filter(reference -> reference.getReferenceElement().getResourceType().equals(ResourceType.MedicationRequest.name()))
                .map(this::extractBasedOn)
                .filter(StringUtils::isNotEmpty)
                .map(this::buildBasedOnCode)
                .collect(Collectors.joining(StringUtils.LF));
        }
        return StringUtils.EMPTY;
    }

    private String extractBasedOn(Reference reference) {
        var resource = messageContext.getInputBundleHolder()
            .getResource(reference.getReferenceElement())
            .map(value -> (MedicationRequest) value)
            .filter(value -> value.getIntent().getDisplay().equals(MedicationRequestIntent.PLAN.getDisplay()));

        if (resource.isPresent()) {
            return messageContext.getMedicationStatementReferenceIdMapper()
                .getOrNew(reference.getIdElement().getId());
        }

        return StringUtils.EMPTY; // TODO: should this throw an exception?
    }

    private String buildBasedOnCode(String id) {
        var inFulfilmentOfTemplateParameters = InFulfilmentOfTemplateParameters.builder()
            .ehrSupplyAuthoriseId(id);

        return TemplateUtils.fillTemplate(IN_FULFILMENT_OF_TEMPLATE, inFulfilmentOfTemplateParameters);
    }
}
