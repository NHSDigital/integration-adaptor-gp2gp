package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationStatementMapper {

    private static final String UK_ZONE_ID = "Europe/London";
    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils
        .loadTemplate("ehr_observation_statement_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_HL7 = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();
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
    private static final String VACCINATION_PROTOCOL_STRING = "Vaccination Protocol %S: %S\nSequence: %S,%S\n";
    private static final String VACCINATION_TARGET_DISEASE = "Target Disease: ";
    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapImmunizationToObservationStatement(Immunization immunization, Bundle bundle, boolean isNested) {
        var observationStatementTemplateParameters = ObservationStatementTemplateParameters.builder()
            .observationStatementId(randomIdGeneratorService.createNewId())
            .availabilityTime(buildAvailabilityTime(immunization))
            .effectiveTime(buildEffectiveTime(immunization))
            .pertinentInformation(buildPertinentInformation(immunization, bundle))
            .isNested(isNested)
            .build();
        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, observationStatementTemplateParameters);
    }

    private String buildAvailabilityTime(Immunization immunization) {
        var dateRecordedExtension = filterExtensionByUrl(immunization, DATE_RECORDED_URL);
        if (dateRecordedExtension.isPresent()) {
            var dateRecorded = dateRecordedExtension.get();
            return formatDateTimeType((DateTimeType) dateRecorded.getValue());
        }
        throw new EhrMapperException("Could not map recorded date");
    }

    private String buildEffectiveTime(Immunization immunization) {
        immunization.getDateElement();
        immunization.getDate();
        Optional<String> effectiveTime = Optional.empty();
        if (immunization.getDate() != null) {
            effectiveTime = Optional.of(formatHL7Date(immunization.getDate()));
        }
        return effectiveTime.orElse(StringUtils.EMPTY);
    }

    private String buildPertinentInformation(Immunization immunization, Bundle bundle) {
        List<String> pertinentInformationList = retrievePertinentInformation(immunization, bundle);
        return pertinentInformationList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining("\n"));
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
        var parentPresentOptional = filterExtensionByUrl(immunization, PARENT_PRESENT_URL);
        if (parentPresentOptional.isPresent()) {
            BooleanType isParentPresent = (BooleanType) parentPresentOptional.get().getValue();
            return PARENT_PRESENT + isParentPresent.getValue();
        }
        return StringUtils.EMPTY;
    }

    private String buildLocationPertinentInformation(Immunization immunization, Bundle bundle) {
        Optional<Location> location = Optional.empty();
        if (immunization.getLocation() != null) {
            String locationId = immunization.getLocation().getReferenceElement().getIdPart();
            location = bundle.getEntry().stream()
                .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Location))
                .map(bundleEntryComponent -> (Location) bundleEntryComponent.getResource())
                .filter(bundleLocation -> bundleLocation.getIdElement().getIdPart().equals(locationId))
                .findFirst();
        }

        return location.map(value -> LOCATION + value.getName()).orElse(StringUtils.EMPTY);
    }

    private String buildManufacturerPertinentInformation(Immunization immunization, Bundle bundle) {
        Optional<Organization> organization = Optional.empty();
        if (immunization.getManufacturer() != null) {
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
        if (immunization.getSite() != null) {
            CodeableConcept siteObject = immunization.getSite();
            site = extractTextOrCoding(siteObject);
        }
        return site.map(value -> SITE + value).orElse(StringUtils.EMPTY);
    }

    private String buildRoutePertinentInformation(Immunization immunization) {
        Optional<String> route = Optional.empty();
        if (immunization.getRoute() != null) {
            CodeableConcept routeObject = immunization.getRoute();
            route = extractTextOrCoding(routeObject);
        }
        return route.map(value -> ROUTE + value).orElse(StringUtils.EMPTY);
    }

    private String buildDoseQuantityPertinentInformation(Immunization immunization) {
        if (immunization.getDoseQuantity().getValue() != null && immunization.getDoseQuantity().getUnit() != null) {
            SimpleQuantity doseQuantity = immunization.getDoseQuantity();
            return doseQuantity.getValue() + StringUtils.SPACE + doseQuantity.getUnit();
        }
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(Immunization immunization) {
        String notes = StringUtils.EMPTY;
        if (ObjectUtils.isNotEmpty(immunization.getNote())) {
            List<Annotation> annotations = immunization.getNote();
            notes = annotations.stream()
                .map(Annotation::getText)
                .collect(Collectors.joining(StringUtils.SPACE));
        }
        return notes;
    }

    private String buildExplanationPertinentInformation(Immunization immunization) {
        Optional<String> explanation;
        if (immunization.getExplanation().getReasonFirstRep().getCodingFirstRep().getDisplay() != null) {
            CodeableConcept reason = immunization.getExplanation().getReasonFirstRep();
            explanation = extractTextOrCoding(reason);
            return explanation.map(value -> REASON + value).orElse(StringUtils.EMPTY);
        } else if (immunization.getExplanation().getReasonNotGivenFirstRep().getCodingFirstRep().getDisplay() != null) {
            CodeableConcept reasonNotGiven = immunization.getExplanation().getReasonNotGivenFirstRep();
            explanation = extractTextOrCoding(reasonNotGiven);
            return explanation.map(value -> REASON_NOT_GIVEN + value).orElse(StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    private String buildVaccinationProtocolPertinentInformation(Immunization immunization) {
        List<Immunization.ImmunizationVaccinationProtocolComponent> vaccinationProtocols = immunization.getVaccinationProtocol();
        AtomicInteger protocolCount = new AtomicInteger(1);
        return vaccinationProtocols.stream()
            .map(protocol -> extractVaccinationProtocolString(protocol, protocolCount))
            .collect(Collectors.joining("\n"));
    }

    private String extractVaccinationProtocolString(Immunization.ImmunizationVaccinationProtocolComponent vaccinationProtocolComponent,
        AtomicInteger protocolCount) {
        String vaccinationProtocol = String.format(VACCINATION_PROTOCOL_STRING,
            protocolCount.getAndIncrement(),
            vaccinationProtocolComponent.getDescription(),
            vaccinationProtocolComponent.getDoseSequence(),
            vaccinationProtocolComponent.getSeriesDoses());

        String targetDiseases = vaccinationProtocolComponent.getTargetDisease().stream()
            .map(this::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(","));

        return vaccinationProtocol + VACCINATION_TARGET_DISEASE + targetDiseases;
    }

    private Optional<String> extractTextOrCoding(CodeableConcept codeableConcept) {
        if (codeableConcept.getText() != null) {
            return Optional.of(codeableConcept.getText());
        } else {
            return Optional.ofNullable(codeableConcept.getCodingFirstRep().getDisplay());
        }
    }

    private Optional<Extension> filterExtensionByUrl(Immunization immunization, String url) {
        return immunization.getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(url))
            .findFirst();
    }

    private String formatDateTimeType(DateTimeType dateTimeType) {
        Date extractedDate = dateTimeType.getValue();
        return formatHL7Date(extractedDate);
    }

    private String formatHL7Date(Date date) {
        return DATE_TIME_FORMATTER_HL7
            .format(date
                .toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }

    private String formatShortDate(Date date) {
        return DATE_TIME_FORMATTER_SHORT
            .format(date
                .toInstant()
                .atZone(ZoneId.of(UK_ZONE_ID))
                .toLocalDateTime());
    }
}
