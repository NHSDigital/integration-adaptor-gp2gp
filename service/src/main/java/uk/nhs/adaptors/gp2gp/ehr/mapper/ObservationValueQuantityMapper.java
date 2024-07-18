package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;

import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.GREATER_OR_EQUAL;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.LESS_OR_EQUAL;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.LESS_THAN;

public final class ObservationValueQuantityMapper {

    private static final String UNITS_OF_MEASURE_SYSTEM =
        "http://unitsofmeasure.org";
    private static final String UNCERTAINTY_EXTENSION =
        "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ValueApproximation-1";
    private static final String UNCERTAINTY_CODE = """
        <uncertaintyCode code="U" codeSystem="2.16.840.1.113883.5.1053" displayName="Recorded as uncertain" />
        """;

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
    public static final String PQ_WITH_ANY_OR_NO_SYSTEM_AND_UNIT_TEMPLATE = """
        <value xsi:type="PQ" value="%s" unit="1">%n\
            <translation value="%s">%n\
                <originalText>%s</originalText>%n\
            </translation>%n\
        </value>""";
    private static final String PQ_WITH_ONLY_VALUE_TEMPLATE =
        "<value xsi:type=\"PQ\" value=\"%s\" unit=\"1\" />";

    private static final String IVL_PQ_WITH_UOM_SYSTEM_AND_CODE_TEMPLATE = """
        <value xsi:type="IVL_PQ">%n\
            <%s value="%s" unit="%s" inclusive="%s" />%n\
        </value>""";

    public static final String IVL_PQ_WITH_NON_UOM_SYSTEM_AND_CODE_AND_UNIT_TEMPLATE = """
        <value xsi:type="IVL_PQ">%n\
            <%s value="%s" unit="%s" inclusive="%s">%n\
                <translation value="%s" code="%s" codeSystem="%s" displayName="%s" />%n\
            </%s>%n\
        </value>""";
    public static final String IVL_PQ_WITH_NON_UOM_SYSTEM_AND_CODE_TEMPLATE = """
        <value xsi:type="IVL_PQ">%n\
            <%s value="%s" unit="%s" inclusive="%s">%n\
                <translation value="%s" code="%s" codeSystem="%s" />%n\
            </%s>%n\
        </value>""";
    public static final String IVL_PQ_WITH_ANY_OR_NO_SYSTEM_AND_UNIT_TEMPLATE = """
        <value xsi:type="IVL_PQ">%n\
             <%s value="%s" unit="1" inclusive="%s">%n\
                <translation value="%s">%n\
                    <originalText>%s</originalText>%n\
                </translation>%n\
            </%s>%n\
        </value>""";
    public static final String IVL_PQ_WITH_ONLY_VALUE_TEMPLATE = """
        <value xsi:type="IVL_PQ">%n\
             <%s value="%s" unit="1" inclusive="%s" />%n\
        </value>""";

    private ObservationValueQuantityMapper() {
    }

    public static String processQuantity(Quantity valueQuantity) {
        var stringBuilder = new StringBuilder();

        if (isUncertaintyCodePresent(valueQuantity)) {
            stringBuilder.append(UNCERTAINTY_CODE);
        }

        var quantityXml = valueQuantity.hasComparator()
            ? getPhysicalQuantityIntervalXml(valueQuantity)
            : getPhysicalQuantityXml(valueQuantity);

        return stringBuilder
            .append(quantityXml)
            .toString();
    }

    private static String getPhysicalQuantityXml(Quantity valueQuantity) {
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
        if (valueQuantity.hasUnit()) {
            return PQ_WITH_ANY_OR_NO_SYSTEM_AND_UNIT_TEMPLATE.formatted(
                valueQuantity.getValue(),
                valueQuantity.getValue(),
                valueQuantity.getUnit()
            );
        }

        return PQ_WITH_ONLY_VALUE_TEMPLATE.formatted(valueQuantity.getValue());
    }

    private static String getPhysicalQuantityIntervalXml(Quantity valueQuantity) {
        if (valueQuantity.hasSystem() && valueQuantity.getSystem().equals(UNITS_OF_MEASURE_SYSTEM)) {
            return IVL_PQ_WITH_UOM_SYSTEM_AND_CODE_TEMPLATE.formatted(
                getHighOrLow(valueQuantity),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                isInclusive(valueQuantity)
            );
        }
        if (valueQuantity.hasSystem() && valueQuantity.hasCode() && valueQuantity.hasUnit()) {
            return IVL_PQ_WITH_NON_UOM_SYSTEM_AND_CODE_AND_UNIT_TEMPLATE.formatted(
                getHighOrLow(valueQuantity),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                isInclusive(valueQuantity),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                valueQuantity.getSystem(),
                valueQuantity.getUnit(),
                getHighOrLow(valueQuantity)
            );
        }
        if (valueQuantity.hasSystem() && valueQuantity.hasCode()) {
            return IVL_PQ_WITH_NON_UOM_SYSTEM_AND_CODE_TEMPLATE.formatted(
                getHighOrLow(valueQuantity),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                isInclusive(valueQuantity),
                valueQuantity.getValue(),
                valueQuantity.getCode(),
                valueQuantity.getSystem(),
                getHighOrLow(valueQuantity)
            );
        }
        if (valueQuantity.hasUnit()) {
            return IVL_PQ_WITH_ANY_OR_NO_SYSTEM_AND_UNIT_TEMPLATE.formatted(
                getHighOrLow(valueQuantity),
                valueQuantity.getValue(),
                isInclusive(valueQuantity),
                valueQuantity.getValue(),
                valueQuantity.getUnit(),
                getHighOrLow(valueQuantity)
            );
        }

        return IVL_PQ_WITH_ONLY_VALUE_TEMPLATE.formatted(
            getHighOrLow(valueQuantity),
            valueQuantity.getValue(),
            isInclusive(valueQuantity)
        );
    }

    private static boolean isInclusive(Quantity valueQuantity) {
        return valueQuantity.getComparator() == GREATER_OR_EQUAL || valueQuantity.getComparator() == LESS_OR_EQUAL;
    }

    private static String getHighOrLow(Quantity valueQuantity) {
        return valueQuantity.getComparator() == LESS_THAN || valueQuantity.getComparator() == LESS_OR_EQUAL
            ? "high"
            : "low";
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
