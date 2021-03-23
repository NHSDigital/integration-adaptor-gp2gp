package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractPlanMedicationRequestReference;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractDispenseRequestQuantityText;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractRepeatValue;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractPrescriptionTypeCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractStatusReasonAvailabilityTime;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractStatusReasonCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestIntent;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestStatus;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.MedicationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class MedicationStatementMapper {
    private static final Mustache MEDICATION_STATEMENT_AUTHORISE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_medication_statement_authorise_template.mustache");
    private static final Mustache MEDICATION_STATEMENT_PRESCRIBE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_medication_statement_prescribe_template.mustache");

    private static final String ACTIVE_STATUS_CODE = "ACTIVE";
    private static final String COMPLETE_STATUS_CODE = "COMPLETE";
    private static final String NOTES = "Notes: %s";
    private static final String EXPECTED_SUPPLY_DURATION = "Expected Supply Duration: %s %s";
    private static final String PATIENT_INSTRUCTION = "Patient Instruction: %s";
    private static final String DEFAULT_QUANTITY_VALUE = "1";
    private static final String DEFAULT_QUANTITY_TEXT = "Unk UoM";
    private static final List<String> ACUTE_PRESCRIPTION_TYPE_CODES = Arrays.asList("acute", "acute-handwritten");
    private static final List<String> REPEAT_PRESCRIPTION_TYPE_CODES =
        Arrays.asList("delayed-prescribing", "repeat", "repeat-dispensing");
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
            .ehrSupplyId(messageContext.getMedicationRequestIdMapper().getOrNew(medicationRequest.getId()))
            .medicationStatementPertinentInformation(buildDosageInstructionPertinentInformation(medicationRequest))
            .ehrSupplyPertinentInformation(buildPertinentInformation(medicationRequest))
            .repeatNumber(buildRepeatValue(medicationRequest))
            .quantityValue(buildQuantityValue(medicationRequest))
            .quantityText(buildQuantityText(medicationRequest))
            .ehrSupplyDiscontinueCode(buildStatusReasonCode(medicationRequest))
            .ehrSupplyDiscontinueId(randomIdGeneratorService.createNewId())
            .ehrSupplyDiscontinueAvailabilityTime(buildStatusReasonAvailabilityTime(medicationRequest))
            .priorPrescriptionId(buildPriorPrescription(medicationRequest))
            .basedOn(buildBasedOn(medicationRequest))
            .build();

        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return TemplateUtils.fillTemplate(MEDICATION_STATEMENT_AUTHORISE_TEMPLATE, medicationStatementTemplateParameters);
        } else if (MedicationRequestIntent.ORDER.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return TemplateUtils.fillTemplate(MEDICATION_STATEMENT_PRESCRIBE_TEMPLATE, medicationStatementTemplateParameters);
        }

        throw new EhrMapperException("Could not map Medication Request intent");
    }

    private String buildStatusCode(MedicationRequest medicationRequest) {
        if (medicationRequest.hasStatus()) {
            if (MedicationRequestStatus.ACTIVE.getDisplay().equals(medicationRequest.getStatus().getDisplay())) {
                return ACTIVE_STATUS_CODE;
            }
            return COMPLETE_STATUS_CODE;
        }
        throw new EhrMapperException("Could not map Medication Request status");
    }

    private String buildMedicationReferenceCode(MedicationRequest medicationRequest) {
        IIdType reference = medicationRequest.getMedicationReference().getReferenceElement();
        return messageContext.getInputBundleHolder()
            .getResource(reference)
            .map(Medication.class::cast)
            .map(Medication::getCode)
            .map(codeableConceptCdMapper::mapCodeableConceptToCd)
            .orElseThrow(() -> new EhrMapperException("Could not resolve Medication Reference"));
    }

    private String buildDosageInstructionPertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDosageInstruction() && medicationRequest.getDosageInstructionFirstRep().hasText()) {
            return medicationRequest.getDosageInstructionFirstRep().getText();
        }
        throw new EhrMapperException("Could not resolve Dosage Instruction text");
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
        if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasQuantity()
            && medicationRequest.getDispenseRequest().getQuantity().hasValue()) {
            return medicationRequest.getDispenseRequest().getQuantity().getValue().toString();
        }
        return DEFAULT_QUANTITY_VALUE;
    }

    private String buildQuantityText(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().getQuantity().hasUnit()) {
            return medicationRequest.getDispenseRequest().getQuantity().getUnit();
        } else if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasExtension()) {
            return extractDispenseRequestQuantityText(medicationRequest);
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
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return extractStatusReasonCode(medicationRequest, codeableConceptCdMapper);
        }
        return StringUtils.EMPTY;
    }

    private String buildStatusReasonAvailabilityTime(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return extractStatusReasonAvailabilityTime(medicationRequest);
        }
        return StringUtils.EMPTY;
    }

    private String buildRepeatValue(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            var prescriptionTypeCode = extractPrescriptionTypeCode(medicationRequest);

            if (ACUTE_PRESCRIPTION_TYPE_CODES.contains(prescriptionTypeCode)) {
                return ACUTE_REPEAT_VALUE;
            } else if (REPEAT_PRESCRIPTION_TYPE_CODES.contains(prescriptionTypeCode)) {
                return extractRepeatValue(medicationRequest);
            }
            throw new EhrMapperException("Could not resolve Prescription Type for Repeat value");
        }
        return StringUtils.EMPTY;
    }

    private String buildBasedOn(MedicationRequest medicationRequest) {
        if (medicationRequest.hasBasedOn()
            && MedicationRequestIntent.ORDER.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return medicationRequest.getBasedOn()
                .stream()
                .filter(reference -> reference.getReferenceElement().getResourceType().equals(ResourceType.MedicationRequest.name()))
                .map(reference -> extractPlanMedicationRequestReference(reference, messageContext))
                .map(MedicationStatementExtractor::buildBasedOnCode)
                .collect(Collectors.joining());
        }
        return StringUtils.EMPTY;
    }

    private String buildPriorPrescription(MedicationRequest medicationRequest) {
        if (medicationRequest.hasPriorPrescription()
            && MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return extractPlanMedicationRequestReference(medicationRequest.getPriorPrescription(), messageContext);
        }
        return StringUtils.EMPTY;
    }
}
