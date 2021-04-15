package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
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
    private static final int COMMENT_OFFSET = 0;
    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";
    private static final Set<String> INTERPRETATION_CODES = Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA");

    private static final String COMPONENT = "Component(s): %s";
    private static final String COMPONENT_CODE = "Code: %s";
    private static final String COMPONENT_REFERENCE_RANGE = "Range: %s %s %s %s";
    private static final String COMPONENT_DELIMITER = "[%s]";

    private static final Map<Class<? extends IBaseElement>, Function<IBaseElement, String>> COMPONENT_VALUE_FUNCTIONS =
        ImmutableMap.of(Quantity.class, value -> processComponentValueQuantity((Quantity) value),
            StringType.class, value -> processComponentValueString((StringType) value));

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final PertinentInformationObservationValueMapper pertinentInformationObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();
        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(idMapper.getOrNew(ResourceType.Observation, observation.getId()))
            .code(prepareCode(observation))
            .comment(prepareComment(observation))
            .issued(DateFormatUtil.toHl7Format(observation.getIssuedElement()))
            .isNested(isNested)
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation));

        if (observation.hasValue()) {
            Type value = observation.getValue();

            if (UNHANDLED_TYPES.contains(value.getClass())) {
                LOGGER.info("Observation value type {} not supported. Mapping for this field is skipped",
                    observation.getValue().getClass());
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
                .ifPresent(coding ->
                    observationStatementTemplateParametersBuilder.interpretation(
                        structuredObservationValueMapper.mapInterpretation(coding))
            );
        }

        if (observation.hasPerformer()) {
            final String participantReference = idMapper.get(observation.getPerformerFirstRep());
            final String participantBlock = participantMapper
                .mapToParticipant(participantReference, ParticipantType.PERFORMER);
            observationStatementTemplateParametersBuilder.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE,
            observationStatementTemplateParametersBuilder.build());
    }

    private String prepareComment(Observation observation) {
        StringBuilder commentBuilder = new StringBuilder(observation.hasComment() ? observation.getComment() : StringUtils.EMPTY);

        if (observation.hasComponent()) {
            commentBuilder.insert(COMMENT_OFFSET, prepareComponentPertinentInformation(observation));
        }

        if (observation.hasValue()  && pertinentInformationObservationValueMapper.isPertinentInformation(observation.getValue())) {
            commentBuilder.insert(COMMENT_OFFSET,
                pertinentInformationObservationValueMapper.mapObservationValueToPertinentInformation(observation.getValue()));
        }

        if (observation.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent referenceRange = observation.getReferenceRangeFirstRep();

            if (observation.hasValueQuantity()) {
                Optional<String> referenceRangeUnit = extractUnit(referenceRange);

                if (referenceRangeUnit.isPresent() && isRangeUnitValid(referenceRangeUnit.get(), observation.getValueQuantity())) {
                    commentBuilder.insert(COMMENT_OFFSET, REFERENCE_RANGE_UNIT_PREFIX + referenceRangeUnit.get() + StringUtils.SPACE);
                }
            } else {
                commentBuilder.insert(COMMENT_OFFSET,
                    pertinentInformationObservationValueMapper.mapReferenceRangeToPertinentInformation(referenceRange));
            }
        }

        if (observation.hasInterpretation()) {
            CodeableConcept interpretation = observation.getInterpretation();

            boolean isInterpretationCode = interpretation.getCoding().stream()
                .anyMatch(this::isInterpretationCode);

            if (!isInterpretationCode) {
                Optional<Coding> codeWithUserSelected = interpretation.getCoding().stream()
                    .filter(code -> !isInterpretationCode(code))
                    .filter(Coding::hasUserSelected)
                    .filter(Coding::getUserSelected)
                    .findFirst();

                Coding coding = codeWithUserSelected.orElseGet(() ->
                    interpretation.hasCoding() ? interpretation.getCodingFirstRep() : null);

                Optional.ofNullable(coding).ifPresent(code -> {
                    if (code.hasDisplay()) {
                        commentBuilder.insert(COMMENT_OFFSET, code.getDisplay() + StringUtils.SPACE);
                    }
                    if (code.hasCode()) {
                        commentBuilder.insert(COMMENT_OFFSET, coding.getCode() + StringUtils.SPACE);
                    }
                    if (code.hasCode() || code.hasDisplay()) {
                        commentBuilder.insert(COMMENT_OFFSET, "Interpretation Code: ");
                    }

                    if (interpretation.hasText()) {
                        commentBuilder.insert(COMMENT_OFFSET, "Interpretation Text: "
                            + interpretation.getText() + StringUtils.SPACE);
                    }
                });
            }
        }

        return commentBuilder.toString();
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

    // TODO: Extract Component Pertinent Information code out of this class?
    // TODO: Fix spacing when building strings
    private String prepareComponentPertinentInformation(Observation observation) {
        String componentPertinentInformation = observation.getComponent()
            .stream()
            .map(this::extractComponentPertinentInformation)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));

        return String.format(COMPONENT, componentPertinentInformation);
    }

    private String extractComponentPertinentInformation(Observation.ObservationComponentComponent component) {
        String componentText = StringUtils.EMPTY;

        if (component.hasCode()) {
            var code = CodeableConceptMappingUtils.extractTextOrCoding(component.getCode());
            if (code.isPresent()) {
                componentText = String.format(COMPONENT_CODE, code.get());
            }
        }

        if (component.hasValue()) {
            var value = component.getValue();

            if (COMPONENT_VALUE_FUNCTIONS.containsKey(value.getClass())) {
                componentText += COMPONENT_VALUE_FUNCTIONS.get(value.getClass()).apply(value);
            } else {
                componentText += pertinentInformationObservationValueMapper.mapObservationValueToPertinentInformation(value);
            }
        }

        if (component.hasInterpretation()) {
            if (component.getInterpretation().hasCoding()) {
                var userSelectedCoding = component
                    .getInterpretation()
                    .getCoding()
                    .stream()
                    .filter(Coding::hasUserSelected)
                    .findFirst();

                if (userSelectedCoding.isPresent()) {
                    componentText += "Interpretation Code: " + userSelectedCoding.get().getCode();
                } else {
                    componentText += "Interpretation Code: " + component.getInterpretation().getCodingFirstRep().getCode();
                }
            }

            if (component.getInterpretation().hasText()) {
                componentText += "Interpretation Text: " + component.getInterpretation().getText();
            }
        }

        if (component.hasReferenceRange()) {
            var referenceRange = component.getReferenceRangeFirstRep();
            if (referenceRange.hasLow() && referenceRange.hasHigh()) {
                var lowValue = referenceRange.getLow().getValue().toString();
                var lowUnit = referenceRange.getLow().getUnit();
                var highValue = referenceRange.getHigh().getValue().toString();
                var highUnit = referenceRange.getHigh().getUnit();

                componentText += String.format(COMPONENT_REFERENCE_RANGE, lowValue, lowUnit, highValue, highUnit);
            }
        }

        if (StringUtils.isNotEmpty(componentText)) {
            return String.format(COMPONENT_DELIMITER, componentText);
        }

        return componentText;
    }

    private static String processComponentValueQuantity(Quantity value) {
        String valueText = "Quantity Value:";

        if (value.hasComparator()) {
            valueText += " " + value.getComparator().getDisplay();
        }
        if (value.hasValue()) {
            valueText += " " + value.getValue().toString();
        }
        if (value.hasUnit()) {
            valueText += " " + value.getUnit();
        }

        return valueText;
    }

    private static String processComponentValueString(StringType value) {
        return "String Value: " + value.getValue();
    }
}
