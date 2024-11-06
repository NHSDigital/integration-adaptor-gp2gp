package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Quantity.QuantityComparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.GREATER_OR_EQUAL;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.GREATER_THAN;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.LESS_OR_EQUAL;
import static org.hl7.fhir.dstu3.model.Quantity.QuantityComparator.LESS_THAN;

public class ObservationValueQuantityMapperTest {

    public static final Extension UNCERTAINTY_EXTENSION = new Extension(
        "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ValueApproximation-1",
        new BooleanType(true)
    );

    public static final double VALUE_37_1 = 37.1;
    public static final String UOM_SYSTEM = "http://unitsofmeasure.org";
    public static final String CODE_CEL = "Cel";
    public static final String UNIT_C = "C";
    public static final String SYSTEM_OID = "1.2.3.4.5";
    public static final String SYSTEM_OID_WITH_PREFIX = "urn:oid:1.2.3.4.5";
    public static final String SYSTEM_UUID = "e1423232-5d4f-472f-8c55-271de1d6f98d";
    public static final String SYSTEM_UUID_WITH_PREFIX = "urn:uuid:e1423232-5d4f-472f-8c55-271de1d6f98d";
    public static final String INVALID_SYSTEM = "not-a-valid-system";
    public static final String STRING_WITH_XML_TO_BE_ESCAPED = "\" ' & < >";

    @Test
    public void When_MappingQuantityWithUncertaintyExtension_Expect_XmlContainsUncertaintyCode() {
        var quantity = (Quantity) new Quantity()
            .setValue(VALUE_37_1)
            .setExtension(List.of(UNCERTAINTY_EXTENSION));

        var expectedXml = """
            <uncertaintyCode code="U" codeSystem="2.16.840.1.113883.5.1053" displayName="Recorded as uncertain" />
            <value xsi:type="PQ" value="37.1" unit="1" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingIntervalWithUncertaintyExtension_Expect_XmlContainsUncertaintyCode() {
        var quantity = (Quantity) new Quantity()
            .setValue(VALUE_37_1)
            .setComparator(GREATER_THAN)
            .setExtension(List.of(UNCERTAINTY_EXTENSION));

        var expectedXml = """
            <uncertaintyCode code="U" codeSystem="2.16.840.1.113883.5.1053" displayName="Recorded as uncertain" />
            <value xsi:type="IVL_PQ">
                <low value="37.1" unit="1" inclusive="false" />
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithNoValue_Expect_EmptyString() {
        var quantity = (Quantity) new Quantity()
            .setSystem(UOM_SYSTEM)
            .setCode(CODE_CEL);
        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEmpty();
    }

    @Test
    public void When_MappingQuantityWithUOMSystemAndCode_Expect_PQWithValueAndUnitSet() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="Cel" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }


    @ParameterizedTest
    @MethodSource("provideComparatorsParameters")
    public void When_MappingIntervalWithUOMSystemAndCode_Expect_IVQPQWithValueAndUnitSet(
        QuantityComparator comparator,
        String label,
        boolean inclusive
    ) {
        var quantity = (Quantity) new Quantity()
            .setComparator(comparator)
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <%s value="37.1" unit="Cel" inclusive="%s" />%n\
            </value>"""
            .formatted(label, inclusive);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithUOMSystemAndUnit_Expect_PQWithValueAndTranslationWithOriginalTextSet() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">
                <translation value="37.1">
                    <originalText>C</originalText>
                </translation>
            </value>""";
        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("provideComparatorsParameters")
    public void When_MappingIntervalWithUOMSystemAndUnit_Expect_IVLPQWithWithValueAndTranslationWithOriginalTextSet(
        QuantityComparator comparator,
        String label,
        boolean inclusive
    ) {
        var quantity = new Quantity()
            .setComparator(comparator)
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <%s value="37.1" unit="1" inclusive="%s">%n\
                    <translation value="37.1">%n\
                        <originalText>C</originalText>%n\
                    </translation>%n\
                </%s>%n\
            </value>"""
            .formatted(label, inclusive, label);
        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithUOMSystemWithoutUnitOrCode_Expect_PQWithValueSetAndUnitSetToOne() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("provideComparatorsParameters")
    public void When_MappingIntervalWithUOMSystemWithoutUnitOrCode_Expect_IVLPQWithValueSetAndUnitSetToOne(
        QuantityComparator comparator,
        String label,
        boolean inclusive
    ) {
        var quantity = new Quantity()
            .setComparator(comparator)
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <%s value="37.1" unit="1" inclusive="%s" />%n\
            </value>"""
            .formatted(label, inclusive);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithInvalidSystemAndWithCodeAndUnit_Expect_PQWithTranslationAndDisplayName() {
        var quantity = new Quantity()
            .setSystem(INVALID_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">
                <translation value="37.1">
                    <originalText>C</originalText>
                </translation>
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("provideComparatorsParameters")
    public void When_MappingIntervalWithInvalidSystemAndWithCodeAndUnit_Expect_IVLPQWithTranslationAndDisplayName(
        QuantityComparator comparator,
        String label,
        boolean inclusive
    ) {
        var quantity = new Quantity()
            .setComparator(comparator)
            .setSystem(INVALID_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <%s value="37.1" unit="1" inclusive="%s">%n\
                    <translation value="37.1">%n\
                        <originalText>C</originalText>%n\
                    </translation>%n\
                </%s>%n\
            </value>"""
            .formatted(label, inclusive, label);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @CsvSource({
        SYSTEM_OID + "," + SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX + "," + SYSTEM_OID,
        SYSTEM_UUID + "," + SYSTEM_UUID,
        SYSTEM_UUID_WITH_PREFIX + "," + SYSTEM_UUID
    })
    public void When_MappingQuantityWithValidNonUOMSystemAndCodeAndUnit_Expect_PQWithTranslationAndDisplayName(
        String system,
        String expected
    ) {
        var quantity = new Quantity()
            .setSystem(system)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">%n\
                <translation value="37.1" code="Cel" codeSystem="%s" displayName="C" />%n\
            </value>"""
            .formatted(expected);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }


    @ParameterizedTest
    @CsvSource({
        SYSTEM_OID + "," + SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX + "," + SYSTEM_OID,
        SYSTEM_UUID + "," + SYSTEM_UUID,
        SYSTEM_UUID_WITH_PREFIX + "," + SYSTEM_UUID
    })
    public void When_MappingIntervalWithValidNonUOMSystemAndCodeAndUnit_Expect_IVLPQWithTranslationAndDisplayName(
        String system,
        String expected
    ) {
        var quantity = new Quantity()
            .setComparator(LESS_THAN)
            .setSystem(system)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <high value="37.1" unit="1" inclusive="false">%n\
                    <translation value="37.1" code="Cel" codeSystem="%s" displayName="C" />%n\
                </high>%n\
            </value>"""
            .formatted(expected);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithInvalidSystemAndCode_Expect_PQValueAndWithUnitSetToOne() {
        var quantity = new Quantity()
            .setSystem(INVALID_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("provideComparatorsParameters")
    public void When_MappingIntervalWithInvalidSystemAndCode_Expect_IVLPQValueAndWithUnitSetToOne(
        QuantityComparator comparator,
        String label,
        boolean inclusive
    ) {
        var quantity = new Quantity()
            .setComparator(comparator)
            .setSystem(INVALID_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <%s value="37.1" unit="1" inclusive="%s" />%n\
            </value>"""
            .formatted(label, inclusive);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }


    @ParameterizedTest
    @CsvSource({
        SYSTEM_OID + "," + SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX + "," + SYSTEM_OID,
        SYSTEM_UUID + "," + SYSTEM_UUID,
        SYSTEM_UUID_WITH_PREFIX + "," + SYSTEM_UUID
    })
    public void When_MappingQuantityWithValidNonUOMSystemAndCode_Expect_PQXmlWithTranslation(
        String system,
        String expected
    ) {
        var quantity = new Quantity()
            .setSystem(system)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">%n\
                <translation value="37.1" code="Cel" codeSystem="%s" />%n\
            </value>"""
            .formatted(expected);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @CsvSource({
        SYSTEM_OID + "," + SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX + "," + SYSTEM_OID,
        SYSTEM_UUID + "," + SYSTEM_UUID,
        SYSTEM_UUID_WITH_PREFIX + "," + SYSTEM_UUID
    })
    public void When_MappingIntervalWithValidNonUOMSystemAndCode_Expect_IVLPQWithTranslation(
        String system,
        String expected
    ) {
        var quantity = new Quantity()
            .setComparator(GREATER_OR_EQUAL)
            .setSystem(system)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="IVL_PQ">%n\
                <low value="37.1" unit="1" inclusive="true">%n\
                    <translation value="37.1" code="Cel" codeSystem="%s" />%n\
                </low>%n\
            </value>"""
            .formatted(expected);

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        UOM_SYSTEM,
        SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX,
        SYSTEM_UUID,
        SYSTEM_UUID_WITH_PREFIX,
        INVALID_SYSTEM
    })
    @NullAndEmptySource
    public void When_MappingQuantityWithAnyOrInvalidOrNoSystemAndHasUnit_Expect_PQWithTranslationAndOriginalText(
        String system
    ) {
        var quantity = new Quantity()
            .setSystem(system)
            .setValue(VALUE_37_1)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">
                <translation value="37.1">
                    <originalText>C</originalText>
                </translation>
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        UOM_SYSTEM,
        SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX,
        SYSTEM_UUID,
        INVALID_SYSTEM
    })
    @NullAndEmptySource
    public void When_MappingIntervalWithAnyOrInvalidOrNoSystemAndHasUnit_Expect_IVLPQWithTranslationAndOriginalText(
        String system
    ) {
        var quantity = new Quantity()
            .setComparator(LESS_OR_EQUAL)
            .setSystem(system)
            .setValue(VALUE_37_1)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="IVL_PQ">
                <high value="37.1" unit="1" inclusive="true">
                    <translation value="37.1">
                        <originalText>C</originalText>
                    </translation>
                </high>
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        UOM_SYSTEM,
        SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX,
        SYSTEM_UUID,
        INVALID_SYSTEM
    })
    @NullAndEmptySource
    public void When_MappingQuantityWithAnyOrInvalidOrNoSystemWithoutUnitOrCode_Expect_PQWithValueSetAndUnitSetToOne(
        String system
    ) {
        var quantity = new Quantity()
            .setSystem(system)
            .setValue(VALUE_37_1);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        UOM_SYSTEM,
        SYSTEM_OID,
        SYSTEM_OID_WITH_PREFIX,
        SYSTEM_UUID,
        INVALID_SYSTEM
    })
    @NullAndEmptySource
    public void When_MappingIntervalWithAnyOrInvalidOrNoSystemWithoutUnitOrCode_Expect_IVLPQWithValueSetAndUnitSetToOne(
        String system
    ) {
        var quantity = new Quantity()
            .setComparator(GREATER_THAN)
            .setSystem(system)
            .setValue(VALUE_37_1);

        var expectedXml = """
            <value xsi:type="IVL_PQ">
                <low value="37.1" unit="1" inclusive="false" />
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingWithXmlCharactersInCode_Expect_XmlCharactersAreEscaped() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(STRING_WITH_XML_TO_BE_ESCAPED);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="&quot; &apos; &amp; &lt; &gt;" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingWithXmlCharactersInUnit_Expect_XmlCharactersAreEscaped() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setUnit(STRING_WITH_XML_TO_BE_ESCAPED);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">
                <translation value="37.1">
                    <originalText>&quot; &apos; &amp; &lt; &gt;</originalText>
                </translation>
            </value>""";
        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    private static Stream<Arguments> provideComparatorsParameters() {
        return Stream.of(
            Arguments.of(LESS_THAN, "high", false),
            Arguments.of(LESS_OR_EQUAL, "high", true),
            Arguments.of(GREATER_THAN, "low", false),
            Arguments.of(GREATER_OR_EQUAL, "low", true)
        );
    }
}
