package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Immunization.ImmunizationPractitionerComponent;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ImmunizationObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toTextFormat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ImmunizationObservationStatementMapper {

    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_immunization_observation_statement_template.mustache");
    private static final String PARENT_PRESENT_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ParentPresent-1";
    private static final String VACCINATION_PROCEDURE_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-VaccinationProcedure-1";
    private static final String PARENT_PRESENT = "Parent Present: ";
    private static final String LOCATION = "Location: ";
    private static final String MANUFACTURER = "Manufacturer: ";
    private static final String BATCH = "Batch: ";
    private static final String EXPIRATION = "Expiration: ";
    private static final String SITE = "Site: ";
    private static final String ROUTE = "Route: ";
    private static final String AP_PRACTITIONER = "AP";
    private static final String QUANTITY = "Quantity: ";
    private static final String REASON = "Reason: ";
    private static final String REASON_NOT_GIVEN = "Reason not given: ";
    private static final String VACCINATION_PROTOCOL_TEMPLATE = "Vaccination Protocol %s: %s Sequence: %s, Doses: %s ";
    private static final String VACCINATION_TARGET_DISEASE = "Target Disease: ";
    private static final String VACCINATION_CODE = "Substance: %s";
    private static final String REPORT_ORIGIN_CODE = "Origin: %s";
    private static final String PRIMARY_SOURCE = "Primary Source: %s";
    private static final String COMMA = ",";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;
    private final ConfidentialityService confidentialityService;

    public String mapImmunizationToObservationStatement(Immunization immunization, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();
        var observationStatementTemplateParameters = ImmunizationObservationStatementTemplateParameters.builder()
            .observationStatementId(idMapper.getOrNew(ResourceType.Immunization, immunization.getIdElement()))
            .availabilityTime(buildAvailabilityTime(immunization))
            .confidentialityCode(confidentialityService.generateConfidentialityCode(immunization).orElse(null))
            .effectiveTime(buildEffectiveTime(immunization))
            .pertinentInformation(buildPertinentInformation(immunization))
            .isNested(isNested)
            .code(buildCode(immunization));

        if (immunization.hasPractitioner() && immunization.getPractitionerFirstRep().hasActor()) {
            var practitioner = extractPractitioner(immunization);
            var practitionerRef = practitioner.getActor();
            var participantRef = messageContext.getAgentDirectory().getAgentId(practitionerRef);
            var participantContent = participantMapper.mapToParticipant(participantRef, ParticipantType.PERFORMER);
            observationStatementTemplateParameters.participant(participantContent);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, observationStatementTemplateParameters.build());
    }

    private String buildAvailabilityTime(Immunization immunization) {
        return retrieveImmunizationDate(immunization);
    }

    private String buildEffectiveTime(Immunization immunization) {
        return retrieveImmunizationDate(immunization);
    }

    private String buildPertinentInformation(Immunization immunization) {
        List<String> pertinentInformationList = retrievePertinentInformation(immunization);
        return pertinentInformationList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrievePertinentInformation(Immunization immunization) {
        return List.of(
            buildReportOriginPertinentInformation(immunization),
            buildParentPresentPertinentInformation(immunization),
            buildPrimarySourcePertinentInformation(immunization),
            buildLocationPertinentInformation(immunization),
            buildManufacturerPertinentInformation(immunization),
            buildLotNumberPertinentInformation(immunization),
            buildExpirationDatePertinentInformation(immunization),
            buildSitePertinentInformation(immunization),
            buildRoutePertinentInformation(immunization),
            buildDoseQuantityPertinentInformation(immunization),
            buildExplanationPertinentInformation(immunization),
            buildVaccinationProtocolPertinentInformation(immunization),
            buildVaccineCode(immunization),
            buildNotePertinentInformation(immunization)
            );
    }

    private String buildReportOriginPertinentInformation(Immunization immunization) {
        if (immunization.hasReportOrigin() && immunization.getReportOrigin().hasCoding()) {
            var code = CodeableConceptMappingUtils.extractTextOrCoding(immunization.getReportOrigin());
            if (code.isPresent()) {
                return String.format(REPORT_ORIGIN_CODE, code.get());
            }
        }

        return StringUtils.EMPTY;
    }

    private String buildParentPresentPertinentInformation(Immunization immunization) {
        var parentPresentOptional = ExtensionMappingUtils.filterExtensionByUrl(immunization, PARENT_PRESENT_URL);
        return parentPresentOptional.map(value -> (BooleanType) value.getValue())
            .map(value -> PARENT_PRESENT + value.getValue())
            .orElse(StringUtils.EMPTY);
    }

    private String buildLocationPertinentInformation(Immunization immunization) {
        if (immunization.hasLocation()) {
            return messageContext.getInputBundleHolder().getResource(immunization.getLocation().getReferenceElement())
                .map(Location.class::cast)
                .map(value -> LOCATION + value.getName())
                .orElse(StringUtils.EMPTY);
        }

        return StringUtils.EMPTY;
    }

    private String buildManufacturerPertinentInformation(Immunization immunization) {
        if (immunization.hasManufacturer()) {
            return messageContext.getInputBundleHolder().getResource(immunization.getManufacturer().getReferenceElement())
                .map(Organization.class::cast)
                .map(value -> MANUFACTURER + value.getName())
                .orElse(StringUtils.EMPTY);
        }

        return StringUtils.EMPTY;
    }

    private String buildLotNumberPertinentInformation(Immunization immunization) {
        Optional<String> batchNumber = Optional.ofNullable(immunization.getLotNumber());
        return batchNumber.map(value -> BATCH + value).orElse(StringUtils.EMPTY);
    }

    private String buildExpirationDatePertinentInformation(Immunization immunization) {
        Optional<DateType> expirationDateElement = Optional.ofNullable(immunization.getExpirationDateElement());
        if (expirationDateElement.isPresent() && expirationDateElement.get().hasValue()) {
            return expirationDateElement.map(dateType -> EXPIRATION + toTextFormat(dateType)).orElse(StringUtils.EMPTY);
        }

        return StringUtils.EMPTY;
    }

    private String buildSitePertinentInformation(Immunization immunization) {
        return Optional.of(immunization)
            .filter(Immunization::hasSite)
            .map(Immunization::getSite)
            .map(CodeableConceptMappingUtils::extractUserSelectedTextOrCoding)
            .filter(Optional::isPresent)
            .map(siteValue -> SITE + siteValue.get())
            .orElse(StringUtils.EMPTY);
    }

    private String buildRoutePertinentInformation(Immunization immunization) {
        Optional<String> route = Optional.empty();
        if (immunization.hasRoute()) {
            CodeableConcept routeObject = immunization.getRoute();
            route = CodeableConceptMappingUtils.extractTextOrCoding(routeObject);
        }
        return route.map(value -> ROUTE + value).orElse(StringUtils.EMPTY);
    }

    private String buildDoseQuantityPertinentInformation(Immunization immunization) {
        StringBuilder quantityInformation = new StringBuilder(QUANTITY);

        if (immunization.hasDoseQuantity()) {
            SimpleQuantity doseQuantity = immunization.getDoseQuantity();

            if (doseQuantity.hasValue()) {
                quantityInformation
                    .append(StringUtils.SPACE)
                    .append(doseQuantity.getValue());
            }

            if (doseQuantity.hasUnit()) {
                quantityInformation
                    .append(StringUtils.SPACE)
                    .append(doseQuantity.getUnit());
            } else if (doseQuantity.hasCode()) {
                quantityInformation
                    .append(StringUtils.SPACE)
                    .append(doseQuantity.getCode());
            }
        }

        return quantityInformation.toString().equals(QUANTITY)
            ? StringUtils.EMPTY
            : quantityInformation.toString();
    }

    private String buildNotePertinentInformation(Immunization immunization) {
        return Stream.concat(
            messageContext.getInputBundleHolder().getRelatedConditions(immunization.getId())
                .stream()
                .map(Condition::getNote)
                .flatMap(List::stream),
            immunization.hasNote() ? immunization.getNote().stream() : Stream.empty()
        )
            .map(Annotation::getText)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String buildExplanationPertinentInformation(Immunization immunization) {

        if (immunization.hasExplanation() && immunization.getExplanation().hasReason()) {
            String reasonGiven = immunization.getExplanation().getReason().stream()
                .map(CodeableConceptMappingUtils::extractTextOrCoding)
                .flatMap(Optional::stream)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(StringUtils.SPACE));
            return StringUtils.isBlank(reasonGiven) ? StringUtils.EMPTY : (REASON + reasonGiven);
        } else if (immunization.hasExplanation() && immunization.getExplanation().hasReasonNotGiven()) {
            String reasonNotGiven = immunization.getExplanation().getReasonNotGiven().stream()
                .map(CodeableConceptMappingUtils::extractTextOrCoding)
                .flatMap(Optional::stream)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(StringUtils.SPACE));
            return StringUtils.isBlank(reasonNotGiven) ? StringUtils.EMPTY : (REASON_NOT_GIVEN + reasonNotGiven);
        }
        return StringUtils.EMPTY;
    }

    private String buildVaccinationProtocolPertinentInformation(Immunization immunization) {
        List<Immunization.ImmunizationVaccinationProtocolComponent> vaccinationProtocols = immunization.getVaccinationProtocol();
        AtomicInteger protocolCount = new AtomicInteger(1);
        return vaccinationProtocols.stream()
            .map(protocol -> extractVaccinationProtocolString(protocol, protocolCount))
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String extractVaccinationProtocolString(Immunization.ImmunizationVaccinationProtocolComponent vaccinationProtocolComponent,
        AtomicInteger protocolCount) {
        String vaccinationProtocol = String.format(VACCINATION_PROTOCOL_TEMPLATE,
            protocolCount.getAndIncrement(),
            vaccinationProtocolComponent.getDescription(),
            vaccinationProtocolComponent.getDoseSequence(),
            vaccinationProtocolComponent.getSeriesDoses());

        String targetDiseases = vaccinationProtocolComponent.getTargetDisease()
            .stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));

        return vaccinationProtocol + VACCINATION_TARGET_DISEASE + targetDiseases;
    }

    private String buildCode(Immunization immunization) {
        var vaccinationProcedure = ExtensionMappingUtils.filterExtensionByUrl(immunization, VACCINATION_PROCEDURE_URL);
        if (vaccinationProcedure.isPresent()) {
            CodeableConcept codeableConcept = (CodeableConcept) vaccinationProcedure.get().getValue();
            return codeableConceptCdMapper.mapCodeableConceptToCd(codeableConcept);
        }
        throw new EhrMapperException("Immunization vaccination procedure not present");
    }

    private String buildVaccineCode(Immunization immunization) {
        if (immunization.hasVaccineCode() && !vaccineCodeUNK(immunization.getVaccineCode())) {
            var code = CodeableConceptMappingUtils.extractTextOrCoding(immunization.getVaccineCode());
            if (code.isPresent()) {
                return String.format(VACCINATION_CODE, code.get());
            }
        } else if (immunization.hasVaccineCode() && vaccineCodeUNK(immunization.getVaccineCode())) {
            return StringUtils.EMPTY;
        }
        throw new EhrMapperException("Immunization vaccine code not present");
    }

    private boolean vaccineCodeUNK(CodeableConcept codeableConcept) {
        return  (codeableConcept.getCodingFirstRep().hasCode() && codeableConcept.getCodingFirstRep().getCode().equals("UNK"));
    }

    private String retrieveImmunizationDate(Immunization immunization) {
        return Optional.of(immunization)
            .filter(Immunization:: hasDateElement)
            .map(Immunization:: getDateElement)
            .map(DateFormatUtil:: toHl7Format)
            .orElse(StringUtils.EMPTY);
    }

    private String buildPrimarySourcePertinentInformation(Immunization immunization) {
        if (immunization.hasPrimarySource()) {
            return String.format(PRIMARY_SOURCE, immunization.getPrimarySource());
        }

        return StringUtils.EMPTY;
    }

    private ImmunizationPractitionerComponent extractPractitioner(Immunization immunization) {
        return immunization.getPractitioner().stream()
            .filter(ImmunizationPractitionerComponent::hasRole)
            .filter(this::hasAPPractitionerRole)
            .findFirst()
            .orElseGet(immunization::getPractitionerFirstRep);
    }

    public boolean hasAPPractitionerRole(ImmunizationPractitionerComponent immunizationPractitionerComponent) {
        return immunizationPractitionerComponent.getRole().getCoding().stream()
            .anyMatch(value -> value.hasCode() && AP_PRACTITIONER.equals(value.getCode()));
    }
}
