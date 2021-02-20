package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class XmlObservationValueMapper {
    private static final Map<Class<? extends Type>, Function<Type, String>> VALUE_MAPPING_FUNCTIONS =
        ImmutableMap.of(Quantity.class, value -> ObservationValueQuantityMapper.processQuantity((Quantity) value),
            StringType.class, value -> processStringType((StringType) value));
    private static final String STRING_VALUE_TEMPLATE = "<value xsi:type=\"ST\">%s</value>";

    public String mapObservationValueToXmlElement(Type value) {
        if (!isXmlValueType(value)) {
            throw new IllegalArgumentException(
                String.format("Observation value of '%s' type can not be converted to xml element", value.getClass()));
        }

        return VALUE_MAPPING_FUNCTIONS.get(value.getClass())
            .apply(value);
    }

    public boolean isXmlValueType(Type value) {
        return VALUE_MAPPING_FUNCTIONS.containsKey(value.getClass());
    }

    private static String processStringType(StringType value) {
        if (value.hasValue()) {
            return String.format(STRING_VALUE_TEMPLATE, value.getValue());
        }
        return StringUtils.EMPTY;
    }
}
