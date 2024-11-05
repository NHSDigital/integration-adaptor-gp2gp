package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;

import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.GREATER_OR_EQUAL;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.LESS_OR_EQUAL;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.LESS_THAN;

public final class ObservationValueQuantityMapper {

    private static final String UNITS_OF_MEASURE_SYSTEM =
        "http://unitsofmeasure.org";
    public static final String URN_OID_PREFIX =
        "urn:oid:";
    public static final String URN_UUID_PREFIX =
        "urn:uuid:";
    public static final String OID_REGEX =
        "(urn:oid:)?[0-2](\\.[1-9]\\d*)+";
    public static final String UUID_REGEX =
        "(urn:uuid:)?[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final String UNCERTAINTY_EXTENSION =
        "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ValueApproximation-1";
    private static final String UNCERTAINTY_CODE = """
        <uncertaintyCode code="U" codeSystem="2.16.840.1.113883.5.1053" displayName="Recorded as uncertain" />
        """;

    private ObservationValueQuantityMapper() {
    }

    public static String processQuantity(Quantity valueQuantity) {
        if (!valueQuantity.hasValue()) {
            return StringUtils.EMPTY;
        }

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
        var escapedCode = StringEscapeUtils.escapeXml11(valueQuantity.getCode());
        var escapedUnit = StringEscapeUtils.escapeXml11(valueQuantity.getUnit());

        if (UNITS_OF_MEASURE_SYSTEM.equals(valueQuantity.getSystem()) && valueQuantity.hasCode()) {
            return """
                <value xsi:type="PQ" value="%s" unit="%s" />"""
                .formatted(
                    valueQuantity.getValue(),
                    escapedCode
                );
        }
        if (hasValidSystem(valueQuantity) && valueQuantity.hasCode() && valueQuantity.hasUnit()) {
            return """
                <value xsi:type="PQ" value="%s" unit="1">%n\
                    <translation value="%s" code="%s" codeSystem="%s" displayName="%s" />%n\
                </value>"""
                .formatted(
                    valueQuantity.getValue(),
                    valueQuantity.getValue(),
                    escapedCode,
                    getSystemWithoutPrefix(valueQuantity.getSystem()),
                    escapedUnit
                );
        }
        if (hasValidSystem(valueQuantity) && valueQuantity.hasCode()) {
            return """
                <value xsi:type="PQ" value="%s" unit="1">%n\
                    <translation value="%s" code="%s" codeSystem="%s" />%n\
                </value>"""
                .formatted(
                    valueQuantity.getValue(),
                    valueQuantity.getValue(),
                    escapedCode,
                    getSystemWithoutPrefix(valueQuantity.getSystem())
                );
        }
        if (valueQuantity.hasUnit()) {
            return """
                <value xsi:type="PQ" value="%s" unit="1">%n\
                    <translation value="%s">%n\
                        <originalText>%s</originalText>%n\
                    </translation>%n\
                </value>"""
                .formatted(
                    valueQuantity.getValue(),
                    valueQuantity.getValue(),
                    escapedUnit
                );
        }

        return """
            <value xsi:type="PQ" value="%s" unit="1" />"""
            .formatted(valueQuantity.getValue());
    }

    private static String getPhysicalQuantityIntervalXml(Quantity valueQuantity) {
        var escapedCode = StringEscapeUtils.escapeXml11(valueQuantity.getCode());
        var escapedUnit = StringEscapeUtils.escapeXml11(valueQuantity.getUnit());

        if (UNITS_OF_MEASURE_SYSTEM.equals(valueQuantity.getSystem()) && valueQuantity.hasCode()) {
            return """
                <value xsi:type="IVL_PQ">%n\
                    <%s value="%s" unit="%s" inclusive="%s" />%n\
                </value>"""
                .formatted(
                    getHighOrLow(valueQuantity),
                    valueQuantity.getValue(),
                    escapedCode,
                    isInclusive(valueQuantity)
                );
        }
        if (hasValidSystem(valueQuantity) && valueQuantity.hasCode() && valueQuantity.hasUnit()) {
            return """
                <value xsi:type="IVL_PQ">%n\
                    <%s value="%s" unit="1" inclusive="%s">%n\
                        <translation value="%s" code="%s" codeSystem="%s" displayName="%s" />%n\
                    </%s>%n\
                </value>"""
                .formatted(
                    getHighOrLow(valueQuantity),
                    valueQuantity.getValue(),
                    isInclusive(valueQuantity),
                    valueQuantity.getValue(),
                    escapedCode,
                    getSystemWithoutPrefix(valueQuantity.getSystem()),
                    escapedUnit,
                    getHighOrLow(valueQuantity)
            );
        }
        if (hasValidSystem(valueQuantity) && valueQuantity.hasCode()) {
            return """
                <value xsi:type="IVL_PQ">%n\
                    <%s value="%s" unit="1" inclusive="%s">%n\
                        <translation value="%s" code="%s" codeSystem="%s" />%n\
                    </%s>%n\
                </value>"""
                .formatted(
                    getHighOrLow(valueQuantity),
                    valueQuantity.getValue(),
                    isInclusive(valueQuantity),
                    valueQuantity.getValue(),
                    escapedCode,
                    getSystemWithoutPrefix(valueQuantity.getSystem()),
                    getHighOrLow(valueQuantity)
                );
        }
        if (valueQuantity.hasUnit()) {
            return """
                <value xsi:type="IVL_PQ">%n\
                    <%s value="%s" unit="1" inclusive="%s">%n\
                        <translation value="%s">%n\
                            <originalText>%s</originalText>%n\
                        </translation>%n\
                    </%s>%n\
                </value>"""
                .formatted(
                    getHighOrLow(valueQuantity),
                    valueQuantity.getValue(),
                    isInclusive(valueQuantity),
                    valueQuantity.getValue(),
                    escapedUnit,
                    getHighOrLow(valueQuantity)
                );
        }

        return """
            <value xsi:type="IVL_PQ">%n\
                <%s value="%s" unit="1" inclusive="%s" />%n\
            </value>"""
            .formatted(
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

    private static boolean hasValidSystem(Quantity valueQuantity) {
        return valueQuantity.hasSystem() && (
            valueQuantity.getSystem().equals(UNITS_OF_MEASURE_SYSTEM)
                || valueQuantity.getSystem().matches(OID_REGEX)
                || valueQuantity.getSystem().matches(UUID_REGEX)
            );
    }

    private static String getSystemWithoutPrefix(String system) {
        if (system.startsWith(URN_OID_PREFIX)) {
            return StringUtils.removeStart(system, URN_OID_PREFIX);
        }
        if (system.startsWith(URN_UUID_PREFIX)) {
            return StringUtils.removeStart(system, URN_UUID_PREFIX);
        }

        return system;
    }
}
