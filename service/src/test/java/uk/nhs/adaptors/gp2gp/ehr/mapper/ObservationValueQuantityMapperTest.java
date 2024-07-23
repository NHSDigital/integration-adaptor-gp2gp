package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservationValueQuantityMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/observation/quantity/";

    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-2.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-2.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-3.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-3.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-4.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR  = TEST_FILES_DIRECTORY
        + "expected-output-quantity-4.xml";
    private static final String INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-5.json";
    private static final String OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-5.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-7.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-7.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-8.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-8.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-9.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-9.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-10.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-10.xml";
    private static final String INPUT_JSON_WITH_COMPARATOR_AND_NO_UNIT = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-12.json";
    private static final String OUTPUT_XML_WITH_COMPARATOR_AND_NO_UNIT = TEST_FILES_DIRECTORY
        + "expected-output-quantity-12.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-13.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-13.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-14.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_LESS_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-14.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-15.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-15.xml";
    private static final String INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "example-observation-resource-with-quantity-16.json";
    private static final String OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_GREATER_COMPARATOR = TEST_FILES_DIRECTORY
        + "expected-output-quantity-16.xml";

    public static final Extension UNCERTAINTY_EXTENSION = new Extension(
        "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ValueApproximation-1",
        new BooleanType(true)
    );

    public static final double VALUE_37_1 = 37.1;
    public static final String UOM_SYSTEM = "http://unitsofmeasure.org";
    public static final String CODE_CEL = "Cel";
    public static final String UNIT_C = "C";
    public static final String NON_UOM_SYSTEM = "Non-UOM system";


    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedQuantityJson_Expect_CorrectXmlOutput(String input, String output) {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = ObservationValueQuantityMapper.processQuantity(observation.getValueQuantity());
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

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
    public void When_MappingQuantityWithNullValue_Expect_EmptyString() {
        var quantity = (Quantity) new Quantity()
            .setSystem(UOM_SYSTEM)
            .setCode(CODE_CEL);
        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEmpty();
    }

    @Test
    public void When_MappingQuantityWithUOMSystemAndCode_Expect_PQXmlWithValueAndQuantitySetAndNoTranslation() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="Cel" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithUOMSystemAndUnit_Expect_PQXmlWithValueAndQuantitySetAndNoTranslation() {
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

    @Test
    public void When_MappingQuantityWithUOMSystemWithoutUnitOrCode_Expect_PQXmlWithValueSetAndUnitSetToOne() {
        var quantity = new Quantity()
            .setSystem(UOM_SYSTEM)
            .setValue(VALUE_37_1);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithNonUOMSystemAndCodeAndUnit_Expect_PQXmlContainsTranslationWithDisplayName() {
        var quantity = new Quantity()
            .setSystem(NON_UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL)
            .setUnit(UNIT_C);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">
                <translation value="37.1" code="Cel" codeSystem="Non-UOM system" displayName="C" />
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithNonUOMSystemAndCode_Expect_PQXmlContainsTranslation() {
        var quantity = new Quantity()
            .setSystem(NON_UOM_SYSTEM)
            .setValue(VALUE_37_1)
            .setCode(CODE_CEL);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1">
                <translation value="37.1" code="Cel" codeSystem="Non-UOM system" />
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        UOM_SYSTEM,
        NON_UOM_SYSTEM
    })
    @NullAndEmptySource
    public void When_MappingQuantityWithAnyOrNoSystemAndHasUnit_Expect_PQXmlContainsTranslationAndOriginalText(String system) {
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
        NON_UOM_SYSTEM
    })
    @NullAndEmptySource
    public void When_MappingQuantityWithAnyOrNoSystemWithoutUnitOrCode_Expect_PQXmlWithValueSetAndUnitSetToOne(String system) {
        var quantity = new Quantity()
            .setSystem(system)
            .setValue(VALUE_37_1);

        var expectedXml = """
            <value xsi:type="PQ" value="37.1" unit="1" />""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingQuantityWithUncertaintyAndComparatorExtension_Expect_XmlContainsUncertaintyCode() {
        var quantity = (Quantity) new Quantity()
            .setValue(VALUE_37_1)
            .setComparator(Quantity.QuantityComparator.GREATER_THAN)
            .setExtension(List.of(UNCERTAINTY_EXTENSION));

        var expectedXml = """
            <uncertaintyCode code="U" codeSystem="2.16.840.1.113883.5.1053" displayName="Recorded as uncertain" />
            <value xsi:type="IVL_PQ">
                 <low value="37.1" unit="1" inclusive="false" />
            </value>""";

        var mappedQuantity = ObservationValueQuantityMapper.processQuantity(quantity);

        assertThat(mappedQuantity).isEqualTo(expectedXml);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_EQUAL_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR,
                OUTPUT_XML_WITH_UNIT_OF_MEASURE_SYSTEM_WITH_GREATER_EQUAL_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_LESS_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_GREATER_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_AND_EQUAL_GREATER_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_COMPARATOR_AND_NO_UNIT,
                OUTPUT_XML_WITH_COMPARATOR_AND_NO_UNIT),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_LESS_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_LESS_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_LESS_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_GREATER_COMPARATOR),
            Arguments.of(INPUT_JSON_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_GREATER_COMPARATOR,
                OUTPUT_XML_WITH_NO_SYSTEM_NO_UNIT_AND_EQUAL_GREATER_COMPARATOR)
        );
    }
}
