package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;

public final class ObservationValueQuantityMapper {

    private static final String UNITS_OF_MEASURE_SYSTEM = "http://unitsofmeasure.org";
    private static final String UNCERTAINTY_EXTENSION = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ValueApproximation-1";

    public static final String PQ_WITH_UOM_SYSTEM_AND_CODE_TEMPLATE = """
        <value xsi:type="PQ" value="%s" unit="%s" />""";
    public static final String PQ_WITH_NON_UOM_SYSTEM_AND_CODE_AND_UNIT_TEMPLATE = """
        <value xsi:type="PQ" value="%s" unit="1">%n\
            <translation value="%s" code="%s" codeSystem="%s" displayName="%s" />%n\
        </value>""";
    public static final String PQ_WITH_NON_UOM_SYSTEM_AND_CODE_TEMPLATE = """
        <value xsi:type="PQ" value="%s" unit="1">%n\
            <translation value="%s" code="%s" codeSystem="%s" />%n\
        </value>""";
    public static final String PQ_WITH_ANY_SYSTEM_AND_UNIT_TEMPLATE = """
        <value xsi:type="PQ" value="%s" unit="1">%n\
            <translation value="%s">%n\
                <originalText>%s</originalText>%n\
            </translation>%n\
        </value>""";
    private static final String PQ_WITH_ONLY_VALUE_TEMPLATE =
        "<value xsi:type=\"PQ\" value=\"%s\" unit=\"1\" />";
    private static final String IVL_PQ_ELEMENT =
        "<value xsi:type=\"IVL_PQ\">";
    private static final String LESS_COMPARATOR_VALUE_TEMPLATE = IVL_PQ_ELEMENT
        + "<high value=\"%s\" unit=\"%s\" inclusive=\"false\"/></value>";
    private static final String LESS_OR_EQUAL_COMPARATOR_VALUE_TEMPLATE = IVL_PQ_ELEMENT
        + "<high value=\"%s\" unit=\"%s\" inclusive=\"true\"/></value>";
    private static final String GREATER_COMPARATOR_VALUE_TEMPLATE = IVL_PQ_ELEMENT + "<low value=\"%s\" "
        + "unit=\"%s\" inclusive=\"false\"/></value>";
    private static final String GREATER_OR_EQUAL_COMPARATOR_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\"><low value=\"%s\" "
        + "unit=\"%s\" inclusive=\"true\"/></value>";
    private static final String LESS_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = IVL_PQ_ELEMENT
        + "<high value=\"%s\" unit=\"1\" inclusive=\"false\"><translation value=\"%s\">"
        + "%s</translation></high></value>";
    private static final String LESS_OR_EQUAL_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = "<value xsi:type=\"IVL_PQ\"><high "
        + "value=\"%s\" unit=\"1\" inclusive=\"true\"><translation value=\"%s\">"
        + "%s</translation></high></value>";
    private static final String GREATER_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = IVL_PQ_ELEMENT
        + "<low value=\"%s\" unit=\"1\" inclusive=\"false\"><translation value=\"%s\">%s"
        + "</translation></low></value>";
    private static final String GREATER_OR_EQUAL_COMPARATOR_NO_SYSTEM_VALUE_TEMPLATE = IVL_PQ_ELEMENT
        + "<low value=\"%s\" unit=\"1\" inclusive=\"true\"><translation value=\"%s\">"
        + "%s</translation></low></value>";
    private static final String UNCERTAINTY_CODE = """
        <uncertaintyCode code="U" codeSystem="2.16.840.1.113883.5.1053" displayName="Recorded as uncertain"/>
        """;
    private static final String QUANTITY_UNIT = "<originalText>%s</originalText>";


    private ObservationValueQuantityMapper() {
    }

    public static String processQuantity(Quantity valueQuantity) {
        var stringBuilder = new StringBuilder();

        if (isUncertaintyCodePresent(valueQuantity)) {
            stringBuilder.append(UNCERTAINTY_CODE);
        }

        if (!valueQuantity.hasComparator()) {
            var result = prepareQuantityValueWithoutComparator(valueQuantity);
            stringBuilder.append(result);
        } else {
            var result = prepareQuantityValueAccordingToComparator(valueQuantity);
            stringBuilder.append(result);
        }

        return stringBuilder.toString();
    }

    private static String prepareQuantityValueWithoutComparator(Quantity valueQuantity) {
        if (UNITS_OF_MEASURE_SYSTEM.equals(valueQuantity.getSystem()) && valueQuantity.hasCode()) {
            return PQ_WITH_UOM_SYSTEM_AND_CODE_TEMPLATE.formatted(
                valueQuantity.getValue(),
                valueQuantity.getCode()
            );
        }
        if (valueQuantity.hasSystem() && valueQuantity.hasCode() && valueQuantity.hasUnit()) {
            return PQ_WITH_NON_UOM_SYSTEM_AND_CODE_AND_UNIT_TEMPLATE.formatted(
                valueQuantity.getValue(),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                valueQuantity.getSystem(),
                valueQuantity.getUnit()
            );
        }
        if (valueQuantity.hasSystem() && valueQuantity.hasCode()) {
            return PQ_WITH_NON_UOM_SYSTEM_AND_CODE_TEMPLATE.formatted(
                valueQuantity.getValue(),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                valueQuantity.getSystem()
            );
        }
        if (valueQuantity.hasSystem() && valueQuantity.hasUnit()) {
            return PQ_WITH_ANY_SYSTEM_AND_UNIT_TEMPLATE.formatted(
                valueQuantity.getValue(),
                valueQuantity.getValue(),
                valueQuantity.getUnit()
            );
        }

        return PQ_WITH_ONLY_VALUE_TEMPLATE.formatted(valueQuantity.getValue());
    }

    private static String prepareUnit(Quantity valueQuantity) {
        return valueQuantity.hasUnit() ? String.format(QUANTITY_UNIT, valueQuantity.getUnit()) : StringUtils.EMPTY;
    }

    private static String prepareQuantityValueAccordingToComparator(Quantity valueQuantity) {
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

    private static String prepareQuantityValueByComparator(Quantity valueQuantity, String systemTemplate, String nonSystemTemplate) {
        if (valueQuantity.hasSystem() && valueQuantity.getSystem().equals(UNITS_OF_MEASURE_SYSTEM)) {
            return formatSystemTemplate(systemTemplate, valueQuantity.getValue(), valueQuantity.getCode());
        }

        return formatNoSystemTemplate(nonSystemTemplate, valueQuantity.getValue(), prepareUnit(valueQuantity));
    }

    private static String formatSystemTemplate(String template, BigDecimal value, String code) {
        return String.format(template, value, code);
    }

    private static String formatNoSystemTemplate(String template, BigDecimal value, String unit) {
        return String.format(template, value, value, unit);
    }

    private static boolean isUncertaintyCodePresent(Quantity valueQuantity) {
        if (valueQuantity.hasExtension()) {
            for (Extension extension : valueQuantity.getExtension()) {
                if (isUncertaintyExtension(extension)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isUncertaintyExtension(Extension extension) {
        return extension.hasUrl()
            && extension.getUrl().equals(UNCERTAINTY_EXTENSION)
            && extension.getValue() instanceof BooleanType
            && ((BooleanType) extension.getValue()).booleanValue();
    }
}
