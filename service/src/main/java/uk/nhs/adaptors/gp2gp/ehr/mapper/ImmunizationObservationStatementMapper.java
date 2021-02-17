package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDateTimeType;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ImmunizationObservationStatementMapper {

    private static final String UK_ZONE_ID = "Europe/London";
    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_observation_statement_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_SHORT = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .toFormatter();
    private static final String PARENT_PRESENT_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-ParentPresent-1";
    private static final String DATE_RECORDED_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-DateRecorded-1";
    private static final String PARENT_PRESENT = "Parent Present: ";
    private static final String LOCATION = "Location: ";
    private static final String MANUFACTURER = "Manufacturer: ";
    private static final String BATCH = "Batch: ";
    private static final String EXPIRATION = "Expiration: ";
    private static final String SITE = "Site: ";
    private static final String ROUTE = "Route: ";
    private static final String QUANTITY = "Quantity: ";
    private static final String REASON = "Reason: ";
    private static final String REASON_NOT_GIVEN = "Reason not given: ";
    private static final String VACCINATION_PROTOCOL_STRING = "Vaccination Protocol %S: %S%nSequence: %S,%S";
    private static final String VACCINATION_TARGET_DISEASE = "Target Disease: ";
    private static final String COMMA = ",";

    private final MessageContext messageContext;

    public String mapImmunizationToObservationStatement(Immunization immunization, Bundle bundle, boolean isNested) {
        var observationStatementTemplateParameters = ImmunizationObservationStatementTemplateParameters.builder()
            .observationStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Immunization, immunization.getId()))
            .availabilityTime(buildAvailabilityTime(immunization))
            .effectiveTime(buildEffectiveTime(immunization))
            .pertinentInformation(buildPertinentInformation(immunization, bundle))
            .isNested(isNested)
            .build();
        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, observationStatementTemplateParameters);
    }

    private String buildAvailabilityTime(Immunization immunization) {
        var dateRecordedExtension = ExtensionMappingUtils.filterExtensionByUrl(immunization, DATE_RECORDED_URL);
        return dateRecordedExtension
            .map(value -> formatDateTimeType((DateTimeType) value.getValue()))
            .orElseThrow(() -> new EhrMapperException("Could not map recorded date"));
    }

    private String buildEffectiveTime(Immunization immunization) {
        Optional<String> effectiveTime = Optional.empty();
        if (immunization.hasDateElement()) {
            effectiveTime = Optional.of(formatDateTimeType(immunization.getDateElement()));
        }
        return effectiveTime.orElse(StringUtils.EMPTY);
    }

    private String buildPertinentInformation(Immunization immunization, Bundle bundle) {
        List<String> pertinentInformationList = retrievePertinentInformation(immunization, bundle);
        return pertinentInformationList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrievePertinentInformation(Immunization immunization, Bundle bundle) {
        return List.of(
            buildParentPresentPertinentInformation(immunization),
            buildLocationPertinentInformation(immunization, bundle),
            buildManufacturerPertinentInformation(immunization, bundle),
            buildLotNumberPertinentInformation(immunization),
            buildExpirationDatePertinentInformation(immunization),
            buildSitePertinentInformation(immunization),
            buildRoutePertinentInformation(immunization),
            buildDoseQuantityPertinentInformation(immunization),
            buildNotePertinentInformation(immunization),
            buildExplanationPertinentInformation(immunization),
            buildVaccinationProtocolPertinentInformation(immunization)
        );
    }

    private String buildParentPresentPertinentInformation(Immunization immunization) {
        var parentPresentOptional = ExtensionMappingUtils.filterExtensionByUrl(immunization, PARENT_PRESENT_URL);
        return parentPresentOptional.map(value -> (BooleanType) value.getValue())
            .map(value -> PARENT_PRESENT + value.getValue())
            .orElse(StringUtils.EMPTY);
    }

    private String buildLocationPertinentInformation(Immunization immunization, Bundle bundle) {
        Optional<Location> location = Optional.empty();
        if (immunization.hasLocation()) {
            String locationId = immunization.getLocation().getReferenceElement().getIdPart();
            location = extractLocationFromBundleById(bundle, locationId);
        }

        return location.map(value -> LOCATION + value.getName())
            .orElse(StringUtils.EMPTY);
    }

    private Optional<Location> extractLocationFromBundleById(Bundle bundle, String locationId) {
        return bundle.getEntry()
            .stream()
            .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Location))
            .map(bundleEntryComponent -> (Location) bundleEntryComponent.getResource())
            .filter(bundleLocation -> bundleLocation.getIdElement().getIdPart().equals(locationId))
            .findFirst();
    }

    private String buildManufacturerPertinentInformation(Immunization immunization, Bundle bundle) {
        Optional<Organization> organization = Optional.empty();
        if (immunization.hasManufacturer()) {
            String organizationId = immunization.getManufacturer().getReferenceElement().getIdPart();
            organization = bundle.getEntry().stream()
                .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Organization))
                .map(bundleEntryComponent -> (Organization) bundleEntryComponent.getResource())
                .filter(bundleOrganization -> bundleOrganization.getIdElement().getIdPart().equals(organizationId))
                .findFirst();
        }
        return organization.map(value -> MANUFACTURER + value.getName()).orElse(StringUtils.EMPTY);
    }

    private String buildLotNumberPertinentInformation(Immunization immunization) {
        Optional<String> batchNumber = Optional.ofNullable(immunization.getLotNumber());
        return batchNumber.map(value -> BATCH + value).orElse(StringUtils.EMPTY);
    }

    private String buildExpirationDatePertinentInformation(Immunization immunization) {
        Optional<Date> expirationDate = Optional.ofNullable(immunization.getExpirationDate());
        return expirationDate.map(value -> EXPIRATION + formatShortDate(value)).orElse(StringUtils.EMPTY);
    }

    private String buildSitePertinentInformation(Immunization immunization) {
        Optional<String> site = Optional.empty();
        if (immunization.hasSite()) {
            CodeableConcept siteObject = immunization.getSite();
            site = CodeableConceptMappingUtils.extractTextOrCoding(siteObject);
        }
        return site.map(value -> SITE + value).orElse(StringUtils.EMPTY);
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
        if (immunization.getDoseQuantity().hasValue() && immunization.getDoseQuantity().hasUnit()) {
            SimpleQuantity doseQuantity = immunization.getDoseQuantity();
            return QUANTITY + doseQuantity.getValue() + StringUtils.SPACE + doseQuantity.getUnit();
        }
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(Immunization immunization) {
        String notes = StringUtils.EMPTY;
        if (immunization.hasNote()) {
            List<Annotation> annotations = immunization.getNote();
            notes = annotations.stream()
                .map(Annotation::getText)
                .collect(Collectors.joining(StringUtils.SPACE));
        }
        return notes;
    }

    private String buildExplanationPertinentInformation(Immunization immunization) {
        Optional<String> explanation;
        if (immunization.getExplanation().getReasonFirstRep().getCodingFirstRep().hasDisplay()) {
            CodeableConcept reason = immunization.getExplanation().getReasonFirstRep();
            explanation = CodeableConceptMappingUtils.extractTextOrCoding(reason);
            return explanation.map(value -> REASON + value).orElse(StringUtils.EMPTY);
        } else if (immunization.getExplanation().getReasonNotGivenFirstRep().getCodingFirstRep().hasDisplay()) {
            CodeableConcept reasonNotGiven = immunization.getExplanation().getReasonNotGivenFirstRep();
            explanation = CodeableConceptMappingUtils.extractTextOrCoding(reasonNotGiven);
            return explanation.map(value -> REASON_NOT_GIVEN + value).orElse(StringUtils.EMPTY);
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
        String vaccinationProtocol = String.format(VACCINATION_PROTOCOL_STRING,
            protocolCount.getAndIncrement(),
            vaccinationProtocolComponent.getDescription(),
            vaccinationProtocolComponent.getDoseSequence(),
            vaccinationProtocolComponent.getSeriesDoses());

        String targetDiseases = vaccinationProtocolComponent.getTargetDisease().stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));

        return vaccinationProtocol + VACCINATION_TARGET_DISEASE + targetDiseases;
    }

    private String formatShortDate(Date date) {
        return DATE_TIME_FORMATTER_SHORT
            .format(date
                .toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }
}
