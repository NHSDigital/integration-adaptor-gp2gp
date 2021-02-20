package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Range;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationValueMapper {
    private static final Map<Class<? extends Type>, Function<Type, String>> VALUE_MAPPING_FUNCTIONS =
        ImmutableMap.of(Quantity.class, value -> ObservationValueQuantityMapper.processQuantity((Quantity) value),
            StringType.class, value -> processStringType((StringType) value));

    private static final Map<Class<? extends Type>, Function<Type, String>> PERTINENT_INFORMATION_APPENDING_FUNCTIONS =
        ImmutableMap.<Class<? extends Type>, Function<Type, String>>builder()
            .put(CodeableConcept.class, value -> processCodeableConcept((CodeableConcept) value))
            .put(BooleanType.class, value -> processBooleanType((BooleanType) value))
            .put(Range.class, value -> processRange((Range) value))
            .put(Ratio.class, value -> processRatio((Ratio) value))
            .put(TimeType.class, value -> processTimeType((TimeType) value))
            .put(DateTimeType.class, value -> processDateTimeType((DateTimeType) value))
            .put(Period.class, value -> processPeriod((Period) value))
            .build();
    private static final List<Class<? extends Type>> OMITTED_TYPES = ImmutableList.of(SampledData.class, Attachment.class);

    private static final String CODEABLE_CONCEPT_VALUE_PREFIX= "Code Value: ";

    public String mapObservationValueToXmlElement(Type value) {
        if (!isXmlValueType(value)) {
            throw new IllegalArgumentException(
                String.format("Observation value of '%s' type can not be converted to xml element", value.getClass()));
        }

        return VALUE_MAPPING_FUNCTIONS.get(value.getClass())
            .apply(value);
    }

    public String mapObservationValueToPertinentInformation(Type value) {
        if (!isPertinentInformation(value)) {
            throw new IllegalArgumentException(
                String.format("Observation value of '%s' type can not be converted to pertinent information", value.getClass()));
        }

        return VALUE_MAPPING_FUNCTIONS.get(value.getClass())
            .apply(value);
    }

    public boolean isXmlValueType(Type value) {
        return VALUE_MAPPING_FUNCTIONS.containsKey(value.getClass());
    }

    public boolean isPertinentInformation(Type value) {
        return PERTINENT_INFORMATION_APPENDING_FUNCTIONS.containsKey(value.getClass());
    }

    private static String processCodeableConcept(CodeableConcept value) {
        if (value.hasCoding() && !value.getCoding().isEmpty()) {
            Coding coding = value.getCoding().get(0);
            if (coding.hasCode() && coding.hasDisplay()) {
                return CODEABLE_CONCEPT_VALUE_PREFIX
                    + coding.getCode()
                    + StringUtils.SPACE
                    + coding.getDisplay()
                    + StringUtils.SPACE;
            }
        }

        return StringUtils.EMPTY;
    }

    private static String processStringType(StringType value) {

        return StringUtils.EMPTY;
    }

    private static String processBooleanType(BooleanType value) {

        return StringUtils.EMPTY;
    }

    private static String processRange(Range value) {

        return StringUtils.EMPTY;
    }

    private static String processRatio(Ratio value) {

        return StringUtils.EMPTY;
    }

    private static String processTimeType(TimeType value) {

        return StringUtils.EMPTY;
    }

    private static String processDateTimeType(DateTimeType value) {

        return StringUtils.EMPTY;
    }

    private static String processPeriod(Period value) {

        return StringUtils.EMPTY;
    }
}
