package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationStatementMapper {

    private static final String UK_ZONE_ID = "Europe/London";
    private static final Mustache OBSERVATION_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_narrative_statement_template.mustache");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmss")
        .toFormatter();
    private static final String PARENT_PRESENT_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-ParentPresent-1";
    private static final String DATE_RECORDED_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-DateRecorded-1";
    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapImmunizationToObservationStatement(Immunization immunization, boolean isNested) {
        var observationStatementTemplateParameters = ObservationStatementTemplateParameters.builder()
            .observationStatementId(randomIdGeneratorService.createNewId())
            .availabilityTime(buildAvailabilityTime(immunization))
            .pertinentInformation(buildPertinentInformation(immunization))
            .isNested(isNested)
            .build();
        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_TEMPLATE, observationStatementTemplateParameters);
    }

    private String buildAvailabilityTime(Immunization immunization) {
        Optional<Extension> dateRecordedExtension = immunization.getExtension().stream().findFirst();
        if (dateRecordedExtension.isPresent()) {
            var dateRecorded = dateRecordedExtension.get();
            parseDateTimeType(dateRecorded.getValue());
        }
        return StringUtils.EMPTY;
    }

    private String parseDateTimeType(Type dateTimeType) {
        System.out.println(dateTimeType);

        return StringUtils.EMPTY;
    }

    private String buildPertinentInformation(Immunization immunization) {
        List<String> pertinentInformationList = retrievePertinentInformation(immunization);
        return StringUtils.EMPTY;
    }

    private List<String> retrievePertinentInformation(Immunization immunization) {
        return List.of(
            buildParentPresentPertinentInformation(immunization),
            buildLocationPertinentInformation(immunization),
            buildManufacturerPertinentInformation(immunization),
            buildLotNumberPertinentInformation(immunization),
            buildExpirationDatePertinentInformation(immunization),
            buildSitePertinentInformation(immunization),
            buildRoutePertinentInformation(immunization),
            buildDoseQuantityPertinentInformation(immunization),
            buildNotePertinentInformation(immunization),
            buildExplanationPertinentInformation(immunization),
            buildVaccinationPertinentInformation(immunization)
        );
    }

    private String buildParentPresentPertinentInformation(Immunization immunization) {
        Optional<Extension> parentPresentOptional = immunization.getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(PARENT_PRESENT_URL))
            .findFirst();

        if (parentPresentOptional.isPresent()) {
            Extension parentPresent = parentPresentOptional.get();
            parentPresent.getValueAsPrimitive().getValue();
        }
        return StringUtils.EMPTY;
    }

    private String buildLocationPertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildManufacturerPertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildLotNumberPertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildExpirationDatePertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildSitePertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildRoutePertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildDoseQuantityPertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildExplanationPertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }

    private String buildVaccinationPertinentInformation(Immunization immunization) {
        return StringUtils.EMPTY;
    }
}
