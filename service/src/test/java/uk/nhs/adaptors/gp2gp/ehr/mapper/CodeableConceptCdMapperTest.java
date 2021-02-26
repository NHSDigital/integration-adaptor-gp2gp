package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

public class CodeableConceptCdMapperTest extends MapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/codeableconcept/";

    private FhirParseService fhirParseService;
    private CodeableConceptCdMapper codeableConceptCdMapper;

    @BeforeEach
    public void setUp() {
        fhirParseService = new FhirParseService();
        codeableConceptCdMapper = new CodeableConceptCdMapper();
    }

    @ParameterizedTest
    @MethodSource("getTestArguments")
    public void When_MappingStubbedCodeableConcept_Expect_HL7CdObjectXml(String inputJson, String outputXml) throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCd(codeableConcept);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> getTestArguments() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY);
    }

}
