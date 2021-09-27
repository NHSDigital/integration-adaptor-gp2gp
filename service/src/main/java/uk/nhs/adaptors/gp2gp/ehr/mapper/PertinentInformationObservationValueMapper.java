package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Range;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class PertinentInformationObservationValueMapper {
    private static final Map<Class<? extends IBaseElement>, Function<IBaseElement, String>> VALUE_TO_PERTINENT_INFORMATION_FUNCTIONS =
        Map.of(
            CodeableConcept.class, value -> processCodeableConcept((CodeableConcept) value),
            BooleanType.class, value -> processBooleanType((BooleanType) value),
            Range.class, value -> processRange((Range) value),
            Ratio.class, value -> processRatio((Ratio) value),
            TimeType.class, value -> processTimeType((TimeType) value),
            DateTimeType.class, value -> processDateTimeType((DateTimeType) value),
            Period.class, value -> processPeriod((Period) value)
    );
    private static final Map<Class<? extends IBaseElement>, Function<IBaseElement, String>> COMPONENT_VALUE_FUNCTIONS =
        Map.of(
            Quantity.class, value -> processComponentValueQuantity((Quantity) value),
            StringType.class, value -> processComponentValueString((StringType) value)
    );
    private static final String CODEABLE_CONCEPT_VALUE_TEMPLATE = "Code Value: %s %s ";
    private static final String BOOLEAN_VALUE_TEMPLATE = "Boolean Value: %s ";
    private static final String RANGE_VALUE_TEMPLATE = "Range Value: %s ";
    private static final String RATIO_VALUE_TEMPLATE = "Ratio Value: %s %s %s / %s %s %s ";
    private static final String TIME_VALUE_TEMPLATE = "Time Value: %s ";
    private static final String DATE_TIME_VALUE_TEMPLATE = "DateTime Value: %s ";
    private static final String PERIOD_VALUE_TEMPLATE = "Period Value: Start %s End %s ";
    private static final String RANGE_PREFIX = "Range: ";
    private static final String TEXT_PREFIX = "Text: ";
    private static final String LOW_PREFIX = "Low: ";
    private static final String HIGH_PREFIX = "High: ";
    private static final String COMPONENT_TEMPLATE = "Component(s): %s ";
    private static final String COMPONENT_CODE_TEMPLATE = "Code: %s";
    private static final String COMPONENT_INTERPRETATION_CODE_TEMPLATE = "Interpretation Code: %s";
    private static final String COMPONENT_INTERPRETATION_TEXT_TEMPLATE = "Interpretation Text: %s";
    private static final String COMPONENT_REFERENCE_RANGE_TEMPLATE = "Range: %s";
    private static final String RANGE_LOW_TEMPLATE = "Low %s %s";
    private static final String RANGE_HIGH_TEMPLATE = "High %s %s";
    private static final String COMPONENT_QUANTITY_VALUE_TEMPLATE = "Quantity Value: %s";
    private static final String COMPONENT_STRING_VALUE_TEMPLATE = "String Value: %s";
    private static final String COMPONENT_DELIMITER_TEMPLATE = "[%s]";

    public String mapObservationValueToPertinentInformation(Type value) {
        if (!isPertinentInformation(value)) {
            throw new IllegalArgumentException(
                String.format("Observation value of '%s' type can not be converted to pertinent information", value.getClass()));
        }

        return VALUE_TO_PERTINENT_INFORMATION_FUNCTIONS.get(value.getClass())
            .apply(value);
    }

    public boolean isPertinentInformation(Type value) {
        return VALUE_TO_PERTINENT_INFORMATION_FUNCTIONS.containsKey(value.getClass());
    }

    public String mapReferenceRangeToPertinentInformation(
            Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent) {
        StringBuilder pertinentInformationBuilder = new StringBuilder(RANGE_PREFIX);

        if (observationReferenceRangeComponent.hasText()) {
            pertinentInformationBuilder.append(TEXT_PREFIX)
                .append(observationReferenceRangeComponent.getText())
                .append(StringUtils.SPACE);
        }

        if (observationReferenceRangeComponent.hasLow()) {
            addRangePertinentInformation(pertinentInformationBuilder,
                observationReferenceRangeComponent.getLow(),
                LOW_PREFIX);
        }
        if (observationReferenceRangeComponent.hasHigh()) {
            addRangePertinentInformation(pertinentInformationBuilder,
                observationReferenceRangeComponent.getHigh(),
                HIGH_PREFIX);
        }

        return pertinentInformationBuilder.toString();
    }

    private void addRangePertinentInformation(StringBuilder pertinentInformationBuilder,
            SimpleQuantity simpleQuantity,
            String valuePrefix) {
        if (simpleQuantity.hasValue()) {
            pertinentInformationBuilder.append(valuePrefix)
                .append(simpleQuantity.getValue())
                .append(StringUtils.SPACE);

            if (simpleQuantity.hasUnit()) {
                pertinentInformationBuilder.append(simpleQuantity.getUnit())
                    .append(StringUtils.SPACE);
            }
        }
    }

    private static String processCodeableConcept(CodeableConcept value) {
        if (value.hasCoding() && !value.getCoding().isEmpty()) {
            Coding coding = value.getCodingFirstRep();
            if (coding.hasCode() && coding.hasDisplay()) {
                return String.format(CODEABLE_CONCEPT_VALUE_TEMPLATE, coding.getCode(), coding.getDisplay());
            }
        }

        return StringUtils.EMPTY;
    }

    private static String processBooleanType(BooleanType value) {
        if (value.hasValue()) {
            return String.format(BOOLEAN_VALUE_TEMPLATE, value.getValue());
        }
        return StringUtils.EMPTY;
    }

    private static String processRange(Range value) {
        List<String> rangeList = List.of(
            extractRangeLow(value),
            extractRangeHigh(value)
        );

        var rangeText = rangeList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));

        if (StringUtils.isNotEmpty(rangeText)) {
            return String.format(RANGE_VALUE_TEMPLATE, rangeText);
        }
        return StringUtils.EMPTY;
    }

    private static String extractRangeLow(Range value) {
        if (value.getLow().hasValue() && value.getLow().hasUnit()) {
            return String.format(RANGE_LOW_TEMPLATE, value.getLow().getValue(), value.getLow().getUnit());
        }
        return StringUtils.EMPTY;
    }

    private static String extractRangeHigh(Range value) {
        if (value.getHigh().hasValue() && value.getHigh().hasUnit()) {
            return String.format(RANGE_HIGH_TEMPLATE, value.getHigh().getValue(), value.getHigh().getUnit());
        }
        return StringUtils.EMPTY;
    }

    private static String processRatio(Ratio value) {
        return String.format(RATIO_VALUE_TEMPLATE,
            value.hasNumerator() ? processQuantity(value.getNumerator()) : StringUtils.EMPTY,
            value.hasDenominator() ? processQuantity(value.getDenominator()) : StringUtils.EMPTY);
    }

    private static String processQuantity(Quantity quantity) {
        return Stream.of(quantity.hasComparator() ? quantity.getComparator().toCode() : null,
                         quantity.getValue(),
                         quantity.getUnit())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining(" "));
    }

    private static String processTimeType(TimeType value) {
        if (value.hasValue()) {
            return String.format(TIME_VALUE_TEMPLATE, value.getValue());
        }

        return StringUtils.EMPTY;
    }

    private static String processDateTimeType(DateTimeType value) {
        if (value.hasValue()) {
            return String.format(DATE_TIME_VALUE_TEMPLATE, DateFormatUtil.toTextFormat(value));
        }

        return StringUtils.EMPTY;
    }

    private static String processPeriod(Period value) {
        if (value.hasStart() && value.hasEnd()) {
            return String.format(PERIOD_VALUE_TEMPLATE,
                DateFormatUtil.toTextFormat(value.getStartElement()),
                DateFormatUtil.toTextFormat(value.getEndElement()));
        }

        return StringUtils.EMPTY;
    }

    public String mapComponentToPertinentInformation(Observation observation) {
        String componentPertinentInformation = observation.getComponent()
            .stream()
            .map(this::buildComponentPertinentInformation)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));

        return String.format(COMPONENT_TEMPLATE, componentPertinentInformation);
    }

    private String buildComponentPertinentInformation(Observation.ObservationComponentComponent component) {
        List<String> componentPertinentInformationList = List.of(
            extractComponentCode(component),
            extractComponentValue(component),
            extractComponentInterpretationCode(component),
            extractComponentInterpretationText(component),
            extractComponentReferenceRange(component)
        );

        var componentText = componentPertinentInformationList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));

        if (StringUtils.isNotEmpty(componentText)) {
            return String.format(COMPONENT_DELIMITER_TEMPLATE, componentText);
        }

        return componentText;
    }

    private String extractComponentCode(Observation.ObservationComponentComponent component) {
        if (component.hasCode()) {
            var code = CodeableConceptMappingUtils.extractTextOrCoding(component.getCode());
            if (code.isPresent()) {
                return String.format(COMPONENT_CODE_TEMPLATE, code.get());
            }
        }
        return StringUtils.EMPTY;
    }

    private String extractComponentValue(Observation.ObservationComponentComponent component) {
        if (component.hasValue()) {
            var value = component.getValue();

            return COMPONENT_VALUE_FUNCTIONS.getOrDefault(value.getClass(),
                $ -> this.mapObservationValueToPertinentInformation(value).trim()
            ).apply(value);
        }
        return StringUtils.EMPTY;
    }

    private String extractComponentInterpretationCode(Observation.ObservationComponentComponent component) {
        if (component.hasInterpretation() && component.getInterpretation().hasCoding()) {
            var userSelectedCoding = component
                .getInterpretation()
                .getCoding()
                .stream()
                .filter(Coding::hasUserSelected)
                .findFirst();

            var codingToUse = userSelectedCoding.orElseGet(() -> component.getInterpretation().getCodingFirstRep());

            return String.format(COMPONENT_INTERPRETATION_CODE_TEMPLATE, codingToUse.getCode());
        }
        return StringUtils.EMPTY;
    }

    private String extractComponentInterpretationText(Observation.ObservationComponentComponent component) {
        if (component.getInterpretation().hasText()) {
            return String.format(COMPONENT_INTERPRETATION_TEXT_TEMPLATE, component.getInterpretation().getText());
        }
        return StringUtils.EMPTY;
    }

    private String extractComponentReferenceRange(Observation.ObservationComponentComponent component) {
        if (component.hasReferenceRange()) {
            List<String> componentReferenceRangeList = List.of(
                extractComponentReferenceRangeLow(component.getReferenceRangeFirstRep()),
                extractComponentReferenceRangeHigh(component.getReferenceRangeFirstRep())
            );

            var componentReferenceRangeText = componentReferenceRangeList.stream()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(StringUtils.SPACE));

            if (StringUtils.isNotEmpty(componentReferenceRangeText)) {
                return String.format(COMPONENT_REFERENCE_RANGE_TEMPLATE, componentReferenceRangeText);
            }
        }
        return StringUtils.EMPTY;
    }

    private static String extractComponentReferenceRangeLow(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.getLow().hasValue() && referenceRange.getLow().hasUnit()) {
            return String.format(RANGE_LOW_TEMPLATE, referenceRange.getLow().getValue(), referenceRange.getLow().getUnit());
        }
        return StringUtils.EMPTY;
    }

    private static String extractComponentReferenceRangeHigh(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.getHigh().hasValue() && referenceRange.getHigh().hasUnit()) {
            return String.format(RANGE_HIGH_TEMPLATE, referenceRange.getHigh().getValue(), referenceRange.getHigh().getUnit());
        }
        return StringUtils.EMPTY;
    }

    private static String extractComponentValueQuantityComparator(Quantity value) {
        if (value.hasComparator()) {
            return value.getComparator().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private static String extractComponentValueQuantity(Quantity value) {
        if (value.hasValue()) {
            return value.getValue().toString();
        }
        return StringUtils.EMPTY;
    }

    private static String extractComponentValueQuantityUnit(Quantity value) {
        if (value.hasUnit()) {
            return value.getUnit();
        }
        return StringUtils.EMPTY;
    }

    private static String processComponentValueQuantity(Quantity value) {
        var valueQuantityList = List.of(
            extractComponentValueQuantityComparator(value),
            extractComponentValueQuantity(value),
            extractComponentValueQuantityUnit(value)
        );

        var valueQuantityText = valueQuantityList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));

        if (StringUtils.isNotEmpty(valueQuantityText)) {
            return String.format(COMPONENT_QUANTITY_VALUE_TEMPLATE, valueQuantityText);
        }

        return StringUtils.EMPTY;
    }

    private static String processComponentValueString(StringType value) {
        return String.format(COMPONENT_STRING_VALUE_TEMPLATE, value.getValue());
    }
}
