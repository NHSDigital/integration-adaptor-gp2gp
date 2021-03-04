package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.BloodPressureParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.BloodPressureParameters.BloodPressureParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;
import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareAvailabilityTimeForBloodPressureNote;
import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareAvailabilityTimeForObservation;
import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareEffectiveTimeForObservation;
import static uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils.loadTemplate;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class BloodPressureMapper {

    private static final Mustache COMPOUND_STATEMENT_BLOOD_PRESSURE_TEMPLATE =
        loadTemplate("ehr_compound_statement_blood_pressure_template.mustache");
    private static final String SYSTOLIC_ARTERIAL_PRESSURE = "72313002";
    private static final String SYSTOLIC_BLOOD_PRESSURE = "271649006";
    private static final String SYSTOLIC_STANDING_BLOOD_PRESSURE = "400974009";
    private static final String SYSTOLIC_SITTING_BLOOD_PRESSURE = "407554009";
    private static final String SYSTOLIC_LYING_BLOOD_PRESSURE = "407556006";
    private static final String DIASTOLIC_ARTERIAL_PRESSURE = "1091811000000102";
    private static final String DIASTOLIC_BLOOD_PRESSURE = "271650006";
    private static final String DIASTOLIC_STANDING_BLOOD_PRESSURE = "400975005";
    private static final String DIASTOLIC_SITTING_BLOOD_PRESSURE = "407555005";
    private static final String DIASTOLIC_LYING_BLOOD_PRESSURE = "407557002";
    private static final List<String> SYSTOLIC_CODE = Arrays.asList(SYSTOLIC_ARTERIAL_PRESSURE,
        SYSTOLIC_BLOOD_PRESSURE, SYSTOLIC_STANDING_BLOOD_PRESSURE, SYSTOLIC_SITTING_BLOOD_PRESSURE, SYSTOLIC_LYING_BLOOD_PRESSURE);
    private static final List<String> DIASTOLIC_CODE = Arrays.asList(DIASTOLIC_ARTERIAL_PRESSURE,
        DIASTOLIC_BLOOD_PRESSURE, DIASTOLIC_STANDING_BLOOD_PRESSURE, DIASTOLIC_SITTING_BLOOD_PRESSURE, DIASTOLIC_LYING_BLOOD_PRESSURE);
    private static final String COMMA = ", ";
    private static final String SYSTOLIC_MEASUREMENT_ABSENT = "Systolic Measurement Absent: %s";
    private static final String DIASTOLIC_MEASUREMENT_ABSENT = "Diastolic Measurement Absent: %s";
    private static final String MEASUREMENT_SITE = "Measurement Site: %s";
    public static final String INTERPRETATION = "Interpretation(s): %s";

    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final StructuredObservationValueMapper structuredObservationValueMapper;

    public String mapBloodPressure(Observation observation, boolean isNested) {
        BloodPressureParametersBuilder builder = BloodPressureParameters.builder()
            .isNested(isNested)
            .id(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
            .effectiveTime(prepareEffectiveTimeForObservation(observation))
            .availabilityTime(prepareAvailabilityTimeForObservation(observation));

        extractBloodPressureComponent(observation, SYSTOLIC_CODE).ifPresent(observationComponent -> {
                builder.systolicId(randomIdGeneratorService.createNewId());
                buildQuantity(observationComponent).ifPresent(builder::systolicQuantity);
                buildReferenceRange(observationComponent).ifPresent(builder::systolicReferenceRange);
            }
        );

        extractBloodPressureComponent(observation, DIASTOLIC_CODE).ifPresent(observationComponent -> {
                builder.diastolicId(randomIdGeneratorService.createNewId());
                buildQuantity(observationComponent).ifPresent(builder::diastolicQuantity);
                buildReferenceRange(observationComponent).ifPresent(builder::diastolicReferenceRange);
            }
        );

        extractNarrativeText(observation).ifPresent(narrativeText -> {
            builder.narrativeId(randomIdGeneratorService.createNewId());
            builder.narrativeText(narrativeText);
            builder.narrativeAvailabilityTime(prepareAvailabilityTimeForBloodPressureNote(observation));
        });

        return TemplateUtils.fillTemplate(COMPOUND_STATEMENT_BLOOD_PRESSURE_TEMPLATE, builder.build());
    }

    private Optional<String> buildReferenceRange(Observation.ObservationComponentComponent observationComponent) {
        if (observationComponent.hasReferenceRange()) {
            return observationComponent.getReferenceRange()
                .stream()
                .map(structuredObservationValueMapper::mapReferenceRangeType)
                .findFirst();
        }

        return Optional.empty();
    }

    private Optional<String> buildQuantity(Observation.ObservationComponentComponent observationComponent) {
        if (observationComponent.hasValueQuantity()) {
            return Optional.of(ObservationValueQuantityMapper.processQuantity(observationComponent.getValueQuantity()));
        }

        return Optional.empty();
    }

    private Optional<Observation.ObservationComponentComponent> extractBloodPressureComponent(Observation observation, List<String> codes) {
        return observation.getComponent()
            .stream()
            .filter(observationComponentComponent -> hasCode(observationComponentComponent, codes))
            .findFirst();
    }

    private boolean hasCode(Observation.ObservationComponentComponent observationComponentComponent, List<String> codes) {
        return observationComponentComponent
            .getCode()
            .getCoding()
            .stream()
            .anyMatch(coding -> codes.contains(coding.getCode()));
    }

    private Optional<String> extractNarrativeText(Observation observation) {
        List<String> narrativeTextParts = new ArrayList<>();

        if (observation.hasComment()) {
            narrativeTextParts.add(observation.getComment());
        }
        extractBodySite(observation).ifPresent(narrativeTextParts::add);

        extractMeasurementAbsent(observation, SYSTOLIC_CODE, SYSTOLIC_MEASUREMENT_ABSENT)
            .ifPresent(narrativeTextParts::add);

        extractMeasurementAbsent(observation, DIASTOLIC_CODE, DIASTOLIC_MEASUREMENT_ABSENT)
            .ifPresent(narrativeTextParts::add);

        extractInterpretation(observation).ifPresent(narrativeTextParts::add);

        return narrativeTextParts.isEmpty() ? Optional.empty() : Optional.of(StringUtils.join(narrativeTextParts, StringUtils.SPACE));
    }

    private Optional<String> extractBodySite(Observation observation) {
        return extractTextOrCoding(observation.getBodySite())
            .map(text -> String.format(MEASUREMENT_SITE, text));
    }

    private Optional<String> extractMeasurementAbsent(Observation observation, List<String> diastolicCode, String measurementSite) {
        return extractBloodPressureComponent(observation, diastolicCode)
            .map(Observation.ObservationComponentComponent::getDataAbsentReason)
            .flatMap(dataAbsentReason -> extractTextOrCoding(dataAbsentReason)
                .map(text -> String.format(measurementSite, text)));
    }

    private Optional<String> extractInterpretation(Observation observation) {
        List<String> interpretations = new ArrayList<>();

        extractTextOrCoding(observation.getInterpretation())
            .ifPresent(interpretations::add);

        extractBloodPressureComponent(observation, SYSTOLIC_CODE)
            .flatMap(observationComponent -> extractTextOrCoding(observationComponent.getInterpretation()))
            .ifPresent(interpretations::add);

        extractBloodPressureComponent(observation, DIASTOLIC_CODE)
            .flatMap(observationComponent -> extractTextOrCoding(observationComponent.getInterpretation()))
            .ifPresent(interpretations::add);

        return interpretations.isEmpty()
            ? Optional.empty() : Optional.of(String.format(INTERPRETATION, StringUtils.join(interpretations, COMMA)));
    }
}
