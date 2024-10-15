package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractDispenseRequestQuantityText;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractDispenseRequestQuantityTextFromQuantity;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractEhrSupplyTypeCodeableConcept;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractIdFromPlanMedicationRequestReference;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractPrescriptionTypeCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractRepeatValue;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractStatusReasonStoppedAvailabilityTime;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractStatusReasonStoppedCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.extractStatusReasonStoppedText;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.hasStatusReasonStopped;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementExtractor.prescriptionTypeTextIsNoInfoAvailable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestIntent;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestStatus;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;

import com.github.mustachejava.Mustache;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.MedicationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.UnitsOfTimeMappingUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class MedicationStatementMapper {
    private static final Mustache MEDICATION_STATEMENT_AUTHORISE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_medication_statement_authorise_template.mustache");
    private static final Mustache MEDICATION_STATEMENT_PRESCRIBE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_medication_statement_prescribe_template.mustache");

    private static final Map<String, Mustache> TEMPLATE_MAPPINGS = Map.of(
        MedicationRequestIntent.PLAN.getDisplay(), MEDICATION_STATEMENT_AUTHORISE_TEMPLATE,
        MedicationRequestIntent.ORDER.getDisplay(), MEDICATION_STATEMENT_PRESCRIBE_TEMPLATE
    );

    private static final Function<String, Optional<Mustache>> DISPLAY_TO_TEMPLATE_MAPPER =
        display -> Optional.ofNullable(TEMPLATE_MAPPINGS.get(display));

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
    private final ParticipantMapper participantMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final ConfidentialityService confidentialityService;

    public String mapMedicationRequestToMedicationStatement(MedicationRequest medicationRequest) {
        var medicationStatementId = messageContext.getIdMapper().getOrNew(ResourceType.MedicationRequest, medicationRequest.getIdElement());
        var statusCode = buildStatusCode(medicationRequest);
        var effectiveTime = StatementTimeMappingUtils.prepareEffectiveTimeForMedicationRequest(medicationRequest);
        var availabilityTime = StatementTimeMappingUtils.prepareAvailabilityTimeForMedicationRequest(medicationRequest);
        var confidentialityCode = confidentialityService.generateConfidentialityCode(medicationRequest)
            .orElse(null);
        var medicationReferenceCode = buildMedicationReferenceCode(medicationRequest);
        var ehrSupplyId = messageContext.getMedicationRequestIdMapper().getOrNew(medicationRequest.getId());
        var medicationStatementPertinentInformation = buildDosageInstructionPertinentInformation(medicationRequest);
        var ehrSupplyPertinentInformation = buildEhrSupplyPertinentInformation(medicationRequest);
        var repeatNumber = buildRepeatNumber(medicationRequest);
        var quantityValue = buildQuantityValue(medicationRequest);
        var quantityText = buildQuantityText(medicationRequest);
        var hasEhrSupplyDiscontinue = hasStatusReasonStopped(medicationRequest);
        var ehrSupplyDiscontinueCode = buildStatusReasonStoppedCode(medicationRequest);
        var ehrSupplyDiscontinueId = randomIdGeneratorService.createNewId();
        var ehrSupplyDiscontinueAvailabilityTime = buildStatusReasonStoppedAvailabilityTime(medicationRequest);
        var ehrSupplyDiscontinueReasonText = buildStatusReasonStoppedText(medicationRequest);
        var basedOn = buildBasedOn(medicationRequest);
        var participant = buildParticipant(medicationRequest);
        var ehrSupplyTypeCode = buildEhrSupplyTypeCode(medicationRequest);

        var medicationStatementTemplateParametersBuilder = MedicationStatementTemplateParameters.builder()
            .medicationStatementId(medicationStatementId)
            .statusCode(statusCode)
            .effectiveTime(effectiveTime)
            .availabilityTime(availabilityTime)
            .confidentialityCode(confidentialityCode)
            .medicationReferenceCode(medicationReferenceCode)
            .ehrSupplyId(ehrSupplyId)
            .medicationStatementPertinentInformation(medicationStatementPertinentInformation)
            .ehrSupplyPertinentInformation(ehrSupplyPertinentInformation)
            .repeatNumber(repeatNumber)
            .quantityValue(quantityValue)
            .quantityText(quantityText)
            .hasEhrSupplyDiscontinue(hasEhrSupplyDiscontinue)
            .ehrSupplyDiscontinueCode(ehrSupplyDiscontinueCode)
            .ehrSupplyDiscontinueId(ehrSupplyDiscontinueId)
            .ehrSupplyDiscontinueAvailabilityTime(ehrSupplyDiscontinueAvailabilityTime)
            .ehrSupplyDiscontinueReasonText(ehrSupplyDiscontinueReasonText)
            .basedOn(basedOn)
            .participant(participant);

        ehrSupplyTypeCode.ifPresent(medicationStatementTemplateParametersBuilder::ehrSupplyTypeCode);

        final var template = DISPLAY_TO_TEMPLATE_MAPPER.apply(medicationRequest.getIntent().getDisplay())
            .orElseThrow(() -> new EhrMapperException("Could not resolve Medication Request intent"));

        return TemplateUtils.fillTemplate(template, medicationStatementTemplateParametersBuilder.build());
    }

    private String buildStatusCode(MedicationRequest medicationRequest) {
        if (medicationRequest.hasStatus()) {
            if (MedicationRequestStatus.ACTIVE.getDisplay().equals(medicationRequest.getStatus().getDisplay())) {
                return ACTIVE_STATUS_CODE;
            }
            return COMPLETE_STATUS_CODE;
        }
        throw new EhrMapperException("Could not resolve Medication Request status");
    }

    private String buildMedicationReferenceCode(MedicationRequest medicationRequest) {
        IIdType reference = medicationRequest.getMedicationReference().getReferenceElement();
        return messageContext.getInputBundleHolder()
            .getResource(reference)
            .map(Medication.class::cast)
            .map(Medication::getCode)
            .map(codeableConceptCdMapper::mapCodeableConceptForMedication)
            .get();
    }

    private String buildDosageInstructionPertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDosageInstruction() && medicationRequest.getDosageInstructionFirstRep().hasText()) {
            return medicationRequest.getDosageInstructionFirstRep().getText();
        }
        throw new EhrMapperException("Could not resolve Dosage Instruction text");
    }

    private String buildEhrSupplyPertinentInformation(MedicationRequest medicationRequest) {
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
        } else if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasQuantity()
            && medicationRequest.getDispenseRequest().getQuantity().hasExtension()) {
            return extractDispenseRequestQuantityTextFromQuantity(medicationRequest);
        }
        return DEFAULT_QUANTITY_TEXT;
    }

    private String buildExpectedSupplyDurationPertinentInformation(MedicationRequest medicationRequest) {
        if (medicationRequest.hasDispenseRequest() && medicationRequest.getDispenseRequest().hasExpectedSupplyDuration()) {
            return String.format(EXPECTED_SUPPLY_DURATION,
                medicationRequest.getDispenseRequest().getExpectedSupplyDuration().getValue().toString(),
                UnitsOfTimeMappingUtils.mapCodeToDisplayValue(
                    medicationRequest.getDispenseRequest().getExpectedSupplyDuration().getCode())
            );
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

    private String buildStatusReasonStoppedCode(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return extractStatusReasonStoppedCode(medicationRequest, codeableConceptCdMapper);
        }
        return StringUtils.EMPTY;
    }

    private String buildStatusReasonStoppedText(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return extractStatusReasonStoppedText(medicationRequest);
        }
        return StringUtils.EMPTY;
    }

    private String buildStatusReasonStoppedAvailabilityTime(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            return extractStatusReasonStoppedAvailabilityTime(medicationRequest);
        }
        return StringUtils.EMPTY;
    }

    private String buildRepeatNumber(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.PLAN.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            var prescriptionTypeCode = extractPrescriptionTypeCode(medicationRequest);

            if (ACUTE_PRESCRIPTION_TYPE_CODES.contains(prescriptionTypeCode)) {
                return ACUTE_REPEAT_VALUE;
            } else if (REPEAT_PRESCRIPTION_TYPE_CODES.contains(prescriptionTypeCode)) {
                return extractRepeatValue(medicationRequest);
            } else if (prescriptionTypeCode.isBlank() && prescriptionTypeTextIsNoInfoAvailable(medicationRequest)) {
                return extractRepeatValue(medicationRequest);
            }
            throw new EhrMapperException("Could not resolve Prescription Type for Repeat value");
        }
        return StringUtils.EMPTY;
    }

    private String buildBasedOn(MedicationRequest medicationRequest) {
        if (MedicationRequestIntent.ORDER.getDisplay().equals(medicationRequest.getIntent().getDisplay())) {
            if (medicationRequest.hasBasedOn()) {
                return medicationRequest.getBasedOn()
                    .stream()
                    .map(reference -> extractIdFromPlanMedicationRequestReference(reference, messageContext))
                    .map(MedicationStatementExtractor::buildBasedOnCode)
                    .collect(Collectors.joining());
            }
            throw new EhrMapperException("Could not resolve Based On for Order Medication Request");
        }
        return StringUtils.EMPTY;
    }

    private String buildParticipant(MedicationRequest medicationRequest) {
        var isPractitioner = buildPredicateReferenceIsA(ResourceType.Practitioner);
        var isPractitionerRole = buildPredicateReferenceIsA(ResourceType.PractitionerRole);
        var isOrganization = buildPredicateReferenceIsA(ResourceType.Organization);
        Predicate<Reference> isRelevant = isPractitioner.or(isPractitionerRole).or(isOrganization);

        if (medicationRequest.hasRequester() && medicationRequest.getRequester().hasAgent()) {
            final var requester = medicationRequest.getRequester();
            final var agent = requester.getAgent();

            if (ResourceType.Practitioner.name().equals(agent.getReferenceElement().getResourceType())
                    && requester.hasOnBehalfOf()) {
                var onBehalfOf = requester.getOnBehalfOf();
                return participantMapper.mapToParticipant(
                    messageContext.getAgentDirectory().getAgentRef(agent, onBehalfOf), ParticipantType.AUTHOR);
            } else {
                return participantMapper.mapToParticipant(
                    messageContext.getAgentDirectory().getAgentId(agent), ParticipantType.AUTHOR);
            }
        } else if (medicationRequest.hasRecorder() && medicationRequest.getRecorder().hasReference()) {
            final var reference = medicationRequest.getRecorder();
            if (isRelevant.test(reference)) {
                return participantMapper.mapToParticipant(
                    messageContext.getAgentDirectory().getAgentId(reference), ParticipantType.AUTHOR);
            }
        }
        return StringUtils.EMPTY;
    }

    private static Predicate<Reference> buildPredicateReferenceIsA(@NonNull ResourceType type) {
        return reference -> type.name().equals(reference.getReferenceElement().getResourceType());
    }

    private Optional<String> buildEhrSupplyTypeCode(MedicationRequest medicationRequest) {
        return extractEhrSupplyTypeCodeableConcept(medicationRequest, messageContext)
            .flatMap(codeableConceptCdMapper::mapCodeableConceptToCdForEhrSupplyType);
    }
}
