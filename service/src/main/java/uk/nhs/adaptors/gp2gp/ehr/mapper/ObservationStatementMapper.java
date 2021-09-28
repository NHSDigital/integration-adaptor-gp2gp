package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper.ConditionWrapper;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class ObservationStatementMapper {
    private static final List<Class<? extends Type>> UNHANDLED_TYPES = ImmutableList.of(SampledData.class, Attachment.class);
    private static final Mustache OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE =
        TemplateUtils.loadTemplate("unstructured_observation_statement_template.mustache");
    private static final String REFERENCE_RANGE_UNIT_PREFIX = "Range Units: ";
    private static final String INTERPRETATION_PREFIX = "Interpretation: ";
    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";
    private static final Set<String> INTERPRETATION_CODES = Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA");

    private boolean interpretationCodeMapped = false;

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final PertinentInformationObservationValueMapper pertinentInformationObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();
        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getIdElement()))
            .code(prepareCode(observation))
            .issued(StatementTimeMappingUtils.prepareAvailabilityTimeForObservation(observation))
            .isNested(isNested)
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation));

        if (observation.hasValue()) {
            Type value = observation.getValue();

            if (UNHANDLED_TYPES.contains(value.getClass())) {
                throw new EhrMapperException(
                    String.format("Observation value type %s not supported.", observation.getValue().getClass()));
            } else if (structuredObservationValueMapper.isStructuredValueType(value)) {
                observationStatementTemplateParametersBuilder.value(
                    structuredObservationValueMapper.mapObservationValueToStructuredElement(value));
            }
        }

        if (observation.hasReferenceRange() && observation.hasValueQuantity()) {
            observationStatementTemplateParametersBuilder.referenceRange(
                structuredObservationValueMapper.mapReferenceRangeType(observation.getReferenceRangeFirstRep()));
        }

        if (observation.hasInterpretation()) {
            observation.getInterpretation().getCoding().stream()
                .filter(this::isInterpretationCode)
                .findFirst()
                .ifPresent(coding -> {
                    String mappedValue = structuredObservationValueMapper.mapInterpretation(coding);
                    if (StringUtils.isNotBlank(mappedValue)) {
                        interpretationCodeMapped = true;
                    }
                    observationStatementTemplateParametersBuilder.interpretation(mappedValue);
                }
            );
        }

        if (observation.hasPerformer()) {
            observationStatementTemplateParametersBuilder.participant(buildParticipant(observation));
        }

        observationStatementTemplateParametersBuilder.comment(prepareComment(observation));

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE,
            observationStatementTemplateParametersBuilder.build());
    }

    private String buildParticipant(Observation observation) {
        Optional<Reference> practitionerReference = getReferenceTo(observation, ResourceType.Practitioner);
        Optional<Reference> organizationReference = getReferenceTo(observation, ResourceType.Organization);

        String participantReference;
        if (practitionerReference.isPresent() && organizationReference.isPresent()) {
            participantReference = messageContext.getAgentDirectory().getAgentRef(practitionerReference.get(), organizationReference.get());
        } else {
            var reference = practitionerReference.orElseGet(() -> organizationReference.orElseThrow(
                () -> new EhrMapperException(
                    "Invalid performer reference in observation resource with id: " + observation.getIdElement().getIdPart()
                )));

            participantReference = messageContext.getAgentDirectory().getAgentId(reference);
        }

        return participantMapper.mapToParticipant(participantReference, ParticipantType.PERFORMER);
    }

    private Optional<Reference> getReferenceTo(Observation observation, ResourceType organization) {
        return observation.getPerformer().stream()
            .filter(Reference::hasReferenceElement)
            .filter(reference -> organization.name().equals(reference.getReferenceElement().getResourceType()))
            .findFirst();
    }

    private String prepareComment(Observation observation) {
        StringBuilder commentBuilder = new StringBuilder();

        if (observation.hasComponent()) {
            commentBuilder.append(pertinentInformationObservationValueMapper.mapComponentToPertinentInformation(observation));
        }

        if (observation.hasValue() && pertinentInformationObservationValueMapper.isPertinentInformation(observation.getValue())) {
            commentBuilder.append(
                pertinentInformationObservationValueMapper.mapObservationValueToPertinentInformation(observation.getValue())
            );
        }

        if (observation.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent referenceRange = observation.getReferenceRangeFirstRep();

            if (observation.hasValueQuantity()) {
                Optional<String> referenceRangeUnit = extractUnit(referenceRange);

                if (referenceRangeUnit.isPresent() && isRangeUnitValid(referenceRangeUnit.get(), observation.getValueQuantity())) {
                    commentBuilder.append(
                        REFERENCE_RANGE_UNIT_PREFIX).append(referenceRangeUnit.get()).append(StringUtils.SPACE);
                }
            } else {
                commentBuilder.append(
                    pertinentInformationObservationValueMapper.mapReferenceRangeToPertinentInformation(referenceRange));
            }
        }

        if (!interpretationCodeMapped && observation.hasInterpretation()) {
            CodeableConceptMappingUtils.extractUserSelectedTextOrCoding(observation.getInterpretation()).ifPresent(interpretationText -> {
                commentBuilder.append(INTERPRETATION_PREFIX).append(interpretationText).append(StringUtils.LF);
            });
        }

        if (observation.hasComment()) {
            commentBuilder.append(observation.getComment());
        }

        buildActualProblemTextIfExists(observation)
            .map(t -> StringUtils.SPACE + t)
            .ifPresent(commentBuilder::append);

        return commentBuilder.toString().trim();
    }

    private Optional<String> buildActualProblemTextIfExists(Observation observation) {
        var problems = messageContext.getInputBundleHolder().getRelatedConditions(observation.getId());

        var text = problems.stream()
            .map(problem -> new ConditionWrapper(problem, messageContext, codeableConceptCdMapper)
                .buildProblemInfo().orElse(StringUtils.EMPTY))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(StringUtils.SPACE));

        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    private Optional<String> extractUnit(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.hasHigh() && referenceRange.getHigh().hasUnit()) {
            return Optional.of(referenceRange.getHigh().getUnit());
        } else if (referenceRange.hasLow() && referenceRange.getLow().hasUnit()) {
            return Optional.of(referenceRange.getLow().getUnit());
        }

        return Optional.empty();
    }

    private String prepareCode(Observation observation) {
        if (observation.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(observation.getCode());
        }
        throw new EhrMapperException("Observation code is not present");
    }

    private boolean isRangeUnitValid(String unit, Quantity quantity) {
        return quantity.hasUnit() && !unit.equals(quantity.getUnit());
    }

    private boolean isInterpretationCode(Coding coding) {
        String codingSystem = coding.getSystem();
        String code = coding.getCode();

        return (coding.hasSystem() && codingSystem.equals(INTERPRETATION_CODE_SYSTEM))
            && INTERPRETATION_CODES.contains(code);
    }
}
