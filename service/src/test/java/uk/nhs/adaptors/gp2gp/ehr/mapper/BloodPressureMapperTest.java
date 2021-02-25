package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BloodPressureMapperTest {
    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String BLOOD_PRESSURE_FILE_LOCATION = "/ehr/mapper/blood_pressure/";

    private BloodPressureMapper bloodPressureMapper;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        bloodPressureMapper = new BloodPressureMapper(messageContext);
    }

    @ParameterizedTest
    @MethodSource("loadTestArguments")
    public void When_MappingBloodPressure_Expect_CompoundStatementXml(String inputJson, String outputXml) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);
        var outputMessage = bloodPressureMapper.mapBloodPressure(observation, false);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> loadTestArguments() {
        return TestArgumentsLoaderUtil.readTestCases(BLOOD_PRESSURE_FILE_LOCATION);
    }
}
