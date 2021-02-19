package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class QuantityObservationMapper {
    private static final String UNITS_OF_MEASURE_SYSTEM = "http://unitsofmeasure.org";
    private static final String UNCERTAINTY_EXTENSION = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ValueApproximation-1";

    private static final String NO_COMPARATOR_VALUE_TEMPLATE = "<value xsi:type=\"PQ\" value=\"%s\" unit=\"%s\"/>";
    private static final String LESS_COMPARATOR_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\">" +
        "<high value=\"%s\" unit=\"%s\" inclusive=\"false\"/></value>";
    private static final String LESS_OR_EQUAL_COMPARATOR_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\">" +
        "<high value=\"%s\" unit=\"%s\" inclusive=\"true\"/></value>";
    private static final String GREATER_COMPARATOR_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\"><low value=\"%s\" " +
        "unit=\"%s\" inclusive=\"false\"/></value>";
    private static final String GREATER_OR_EQUAL_COMPARATOR_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\"><low value=\"%s\" " +
        "unit=\"%s\" inclusive=\"true\"/></value>";
    private static final String NO_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = "<value xsi:type=\"PQ\" value=\"valueQuantity.value\" unit=\"1\">" +
        "<translation value=\"valueQuantity.value\"><originalText>%s</originalText></translation></value>";
    private static final String LESS_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\">" +
        "<high value=\"%s\" unit=\"1\" inclusive=\"false\"><translation value=\"%s\">" +
        "<originalText>%s</originalText></translation></high></value>";
    private static final String LESS_OR_EQUAL_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\"><high " +
        "value=\"%s\" unit=\"1\" inclusive=\"true\"><translation value=\"%s\">" +
        "<originalText>%s</originalText></translation></high></value>";
    private static final String GREATER_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\">" +
        "<low value=\"%s\" unit=\"1\" inclusive=\"false\"><translation value=\"%s\">%s</originalText></translation></low></value>";
    private static final String GREATER_OR_EQUAL_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\">" +
        "<low value=\"%s\" unit=\"1\" inclusive=\"true\"><translation value=\"%s\">" +
        "<originalText>%s</originalText></translation></low></value>";
    private static final String UNCERTAINTY_CODE = "<uncertaintyCode code=\"U\" " +
        "codeSystem=\"2.16.840.1.113883.5.1053\" displayName=\"Recorded as uncertain\"/>";

    public String mapObservationValueQuantity(Quantity valueQuantity) {
        String result = StringUtils.EMPTY;

        if (isUncertaintyCodePresent(valueQuantity)) {
            result += UNCERTAINTY_CODE;
        }

        if (!valueQuantity.hasComparator()) {
            result += prepareQuantityValueWithoutComparator(valueQuantity);
        } else {
            result += prepareQuantityValueAccordingToComparator(valueQuantity);
        }

        return result;
    }

    private String prepareQuantityValueWithoutComparator(Quantity valueQuantity) {
        if (valueQuantity.hasSystem() && valueQuantity.getSystem().equals(UNITS_OF_MEASURE_SYSTEM)) {
            return String.format(NO_COMPARATOR_VALUE_TEMPLATE, valueQuantity.getValue(), valueQuantity.getCode());
        } else {
            return String.format(NO_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE, valueQuantity.getUnit());
        }
    }

    private String prepareQuantityValueAccordingToComparator(Quantity valueQuantity) {
        if (valueQuantity.getComparator() == Quantity.QuantityComparator.LESS_THAN) {
            return prepareQuantityValueByComparator(valueQuantity,
                LESS_COMPARATOR_VALUE_TEMPLATE,
                LESS_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE);
        } else if (valueQuantity.getComparator() == Quantity.QuantityComparator.LESS_OR_EQUAL) {
            return prepareQuantityValueByComparator(valueQuantity,
                LESS_OR_EQUAL_COMPARATOR_VALUE_TEMPLATE,
                LESS_OR_EQUAL_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE);
        } else if (valueQuantity.getComparator() == Quantity.QuantityComparator.GREATER_THAN) {
            return prepareQuantityValueByComparator(valueQuantity,
                GREATER_COMPARATOR_VALUE_TEMPLATE,
                GREATER_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE);
        } else if (valueQuantity.getComparator() == Quantity.QuantityComparator.GREATER_OR_EQUAL) {
            return prepareQuantityValueByComparator(valueQuantity,
                GREATER_OR_EQUAL_COMPARATOR_VALUE_TEMPLATE,
                GREATER_OR_EQUAL_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE);
        }

        return StringUtils.EMPTY;
    }

    private String prepareQuantityValueByComparator(Quantity valueQuantity, String systemTemplate, String nonSystemTemplate) {
        if (valueQuantity.hasSystem() && valueQuantity.getSystem().equals(UNITS_OF_MEASURE_SYSTEM)) {
            return formatSystemTemplate(systemTemplate, valueQuantity.getValue(), valueQuantity.getCode());
        }

        return formatNoSystemTemplate(nonSystemTemplate, valueQuantity.getValue(), valueQuantity.getUnit());
    }

    private String formatSystemTemplate(String template, BigDecimal value, String code) {
        return String.format(template, value, code);
    }

    private String formatNoSystemTemplate(String template, BigDecimal value, String unit) {
        return String.format(template, value, value, unit);
    }

    private boolean isUncertaintyCodePresent(Quantity valueQuantity) {
        if (valueQuantity.hasExtension()) {
            for (Extension extension : valueQuantity.getExtension()) {
                if (extension.hasUrl() && extension.getUrl().equals(UNCERTAINTY_EXTENSION)) {
                    return true;
                }
            }
        }
        return false;
    }
}
