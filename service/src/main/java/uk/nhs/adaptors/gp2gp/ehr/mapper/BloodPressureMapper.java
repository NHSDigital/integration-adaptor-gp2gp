package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.BloodPressureParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.BloodPressureParameters.BloodPressureParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;
import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareAvailabilityTimeForObservation;
import static uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils.prepareEffectiveTimeForObservation;
import static uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils.loadTemplate;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class BloodPressureMapper {

    private static final Mustache COMPOUND_STATEMENT_BLOOD_PRESSURE_TEMPLATE =
            loadTemplate("ehr_compound_statement_blood_pressure_template.mustache");

    private final MessageContext messageContext;


    public String mapBloodPressure(Observation observation, boolean isNested) {
        BloodPressureParametersBuilder builder = BloodPressureParameters.builder()
                .isNested(isNested)
                .id(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
                .effectiveTime(prepareEffectiveTimeForObservation(observation))
                .availabilityTime(prepareAvailabilityTimeForObservation(observation));

        getNarrativeText(observation).ifPresent(builder::narrativeText);

        return TemplateUtils.fillTemplate(COMPOUND_STATEMENT_BLOOD_PRESSURE_TEMPLATE, builder.build());
    }

    private Optional<String> getNarrativeText(Observation observation) {
        List<String> narrativeTextParts = new ArrayList<>();

        if (StringUtils.isNotEmpty(observation.getComment())) {
            narrativeTextParts.add(observation.getComment());
        }

        if (systolicMeasurementPresent(observation)) {
            narrativeTextParts.add(extractCodeableConceptText(observation.getComponentFirstRep().getDataAbsentReason()));
        }

        if (diastolicMeasurementPresent(observation)) {
            narrativeTextParts.add(extractCodeableConceptText(observation.getComponent().get(1).getDataAbsentReason()));
        }

        narrativeTextParts.add(extractInterpretation(observation));

        extractBodySite(observation)
                .ifPresent(narrativeTextParts::add);

        if (!narrativeTextParts.isEmpty()) {
            return Optional.of(narrativeTextParts.stream().collect(joining("; ")));
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractBodySite(Observation observation) {
        if (observation.hasBodySite()) {
            return Optional.of(extractCodeableConceptText(observation.getBodySite()));
        }

        return Optional.empty();
    }

    private String extractInterpretation(Observation observation) {
        List<String> interpretations = new ArrayList<>();

        if (observation.hasComponent()) {
            if (observation.getComponentFirstRep().hasInterpretation()) {
                interpretations.add(extractCodeableConceptText(observation.getComponentFirstRep().getInterpretation()));
            }

            if (observation.getComponent().size() > 1 && observation.getComponent().get(1).hasInterpretation()) {
                interpretations.add(extractCodeableConceptText(observation.getComponent().get(1).getInterpretation()));
            }
        }

        return interpretations.stream()
                .map(it -> '{' + it + '}')
                .collect(joining(", "));
    }

    private boolean systolicMeasurementPresent(Observation observation) {
        return observation.hasComponent() && !observation.getComponentFirstRep().hasDataAbsentReason();
    }

    private boolean diastolicMeasurementPresent(Observation observation) {
        return observation.hasComponent() && observation.getComponent().size() > 1
                && !observation.getComponent().get(1).hasDataAbsentReason();
    }

    private static String extractCodeableConceptText(CodeableConcept codeableConcept) {
        if (codeableConcept.hasText()) {
            return codeableConcept.getText();
        } else {
            return codeableConcept.getCodingFirstRep().getDisplay();
        }
    }
}
