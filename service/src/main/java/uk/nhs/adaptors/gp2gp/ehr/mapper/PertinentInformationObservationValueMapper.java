package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Map;
import java.util.function.Function;

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
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class PertinentInformationObservationValueMapper {
    private static final Map<Class<? extends IBaseElement>, Function<IBaseElement, String>> VALUE_TO_PERTINENT_INFORMATION_FUNCTIONS =
        ImmutableMap.<Class<? extends IBaseElement>, Function<IBaseElement, String>>builder()
            .put(CodeableConcept.class, value -> processCodeableConcept((CodeableConcept) value))
            .put(BooleanType.class, value -> processBooleanType((BooleanType) value))
            .put(Range.class, value -> processRange((Range) value))
            .put(Ratio.class, value -> processRatio((Ratio) value))
            .put(TimeType.class, value -> processTimeType((TimeType) value))
            .put(DateTimeType.class, value -> processDateTimeType((DateTimeType) value))
            .put(Period.class, value -> processPeriod((Period) value))
            .build();
    private static final String CODEABLE_CONCEPT_VALUE_TEMPLATE = "Code Value: %s %s ";
    private static final String BOOLEAN_VALUE_TEMPLATE = "Boolean Value: %s ";
    private static final String RANGE_VALUE_TEMPLATE = "Range Value: Low %s %s High %s %s ";
    private static final String RATIO_VALUE_TEMPLATE = "Ratio Value: %s %s %s / %s %s %s ";
    private static final String TIME_VALUE_TEMPLATE = "Time Value: %s ";
    private static final String DATE_TIME_VALUE_TEMPLATE = "DateTime Value: %s ";
    private static final String PERIOD_VALUE_TEMPLATE = "Period Value: Start %s End %s ";

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

    private static String processCodeableConcept(CodeableConcept value) {
        if (value.hasCoding() && !value.getCoding().isEmpty()) {
            Coding coding = value.getCoding().get(0);
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
        if (isLowValuePresent(value) && isHighValuePresent(value)) {
            return String.format(RANGE_VALUE_TEMPLATE,
                value.getLow().getValue(),
                value.getLow().getUnit(),
                value.getHigh().getValue(),
                value.getHigh().getUnit());
        }
        return StringUtils.EMPTY;
    }

    private static String processRatio(Ratio value) {
        if (isNumeratorPresent(value) && isDenominatorPresent(value)) {
            Quantity numerator = value.getNumerator();
            Quantity denominator = value.getDenominator();

            return String.format(RATIO_VALUE_TEMPLATE,
                numerator.getComparator().toCode(),
                numerator.getValue(),
                numerator.getUnit(),
                denominator.getComparator().toCode(),
                denominator.getValue(),
                denominator.getUnit());
        }

        return StringUtils.EMPTY;
    }

    private static String processTimeType(TimeType value) {
        if (value.hasValue()) {
            return String.format(TIME_VALUE_TEMPLATE, value.getValue());
        }

        return StringUtils.EMPTY;
    }

    private static String processDateTimeType(DateTimeType value) {
        if (value.hasValue()) {
            return String.format(DATE_TIME_VALUE_TEMPLATE, value.getValue());
        }

        return StringUtils.EMPTY;
    }

    private static String processPeriod(Period value) {
        if (value.hasStart() && value.hasEnd()) {
            return String.format(PERIOD_VALUE_TEMPLATE, value.getStart(), value.getEnd());
        }

        return StringUtils.EMPTY;
    }

    private static boolean isLowValuePresent(Range range) {
        return range.hasLow()
            && range.getLow().hasValue()
            && range.getLow().hasUnit();
    }

    private static boolean isHighValuePresent(Range range) {
        return range.hasHigh()
            && range.getHigh().hasValue()
            && range.getHigh().hasUnit();
    }

    private static boolean isNumeratorPresent(Ratio ratio) {
        return ratio.hasNumerator()
            && ratio.getNumerator().hasComparator()
            && ratio.getNumerator().hasValue()
            && ratio.getNumerator().hasUnit();
    }

    private static boolean isDenominatorPresent(Ratio ratio) {
        return ratio.hasDenominator()
            && ratio.getDenominator().hasComparator()
            && ratio.getDenominator().hasValue()
            && ratio.getDenominator().hasUnit();
    }

    private static String mapReferenceRangeToPertinentInformation(
            Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent) {
        String result = "Range: ";
        if (observationReferenceRangeComponent.hasText()) {
            result += "Text: " + observationReferenceRangeComponent.getText();

            if (observationReferenceRangeComponent.hasLow()) {
                SimpleQuantity low = observationReferenceRangeComponent.getLow();
                if (low.hasValue()) {
                    result += "Low: " + low.getValue() + StringUtils.SPACE;

                    if (low.hasUnit()) {
                        result += low.getUnit() + StringUtils.SPACE;
                    }
                }
            }

            if (observationReferenceRangeComponent.hasHigh()) {
                SimpleQuantity high = observationReferenceRangeComponent.getHigh();
                if (high.hasValue()) {
                    result += "High: " + high.getValue() + StringUtils.SPACE;

                    if (high.hasUnit()) {
                        result += high.getUnit() + StringUtils.SPACE;
                    }
                }
            }
        }

        return result;
    }
}
