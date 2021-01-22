package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskIdService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrFhirExtractParams;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EhrExtractMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;
    private static final String XML_INPUT_FILE_NAME = "06-tpp-req-pretty.xml";
    private static final String JSON_INPUT_FILE_NAME = "gpc-access-structured.json";
    private static final String EXPECTED_XML_TO_XML_FILE_NAME = "ExpectedEhrExtractResponseFromXml.xml";
    private static final String EXPECTED_XML_TO_JSON_FILE_NAME = "ExpectedEhrExtractResponseFromJson.xml";
    private static final String TEST_ID = "test-id";
    private static final String JSON_WITH_NO_CONTENT = "{}";

    private static String inputXmlFileContent;
    private static String inputJsonFileContent;
    private static String expectedXmlToXmlContent;
    private static String expectedJsonToXmlContent;

    @Mock
    private TaskIdService taskIdService;

    private EhrExtractMapper ehrExtractMapper;

    @BeforeAll
    public static void initialize() throws IOException {
        inputXmlFileContent = getFileContent(INPUT_PATH + XML_INPUT_FILE_NAME);
        inputJsonFileContent = getFileContent(INPUT_PATH + JSON_INPUT_FILE_NAME);
        expectedXmlToXmlContent = getFileContent(OUTPUT_PATH + EXPECTED_XML_TO_XML_FILE_NAME);
        expectedJsonToXmlContent = getFileContent(OUTPUT_PATH + EXPECTED_XML_TO_JSON_FILE_NAME);
    }

    @BeforeEach
    public void setUp() {
        when(taskIdService.createNewTaskId()).thenReturn(TEST_ID);
        ehrExtractMapper = new EhrExtractMapper(new XPathService(), new FhirParseService(), taskIdService);
    }

    @Test
    public void When_MappingProperXmlRequestBody_Expect_ProperXmlOutput() {
        EhrFhirExtractParams ehrFhirExtractParams = ehrExtractMapper.mapXmlToEhrFhirExtractParams(inputXmlFileContent);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrFhirExtractParams);

        assertThat(output).isEqualToIgnoringWhitespace(expectedXmlToXmlContent);
    }

    @Test
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput() {
        EhrFhirExtractParams ehrFhirExtractParams = ehrExtractMapper.mapJsonToEhrFhirExtractParams(inputJsonFileContent);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrFhirExtractParams);

        assertThat(output).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }

    @Test
    public void When_MappingEmptyXMLRequestBody_Expect_IllegalArgumentExceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> ehrExtractMapper.mapXmlToEhrFhirExtractParams(StringUtils.EMPTY));
    }

    @Test
    public void When_MappingEmptyJsonRequestBody_Expect_FhirValidationExceptionThrown() {
        assertThrows(FhirValidationException.class, () -> ehrExtractMapper.mapJsonToEhrFhirExtractParams(StringUtils.EMPTY));
    }

    @Test
    public void When_MappingJsonBodyWithNoContentRequestBody_Expect_FhirValidationExceptionThrown() {
        assertThrows(FhirValidationException.class, () -> ehrExtractMapper.mapJsonToEhrFhirExtractParams(JSON_WITH_NO_CONTENT));
    }

    private static String getFileContent(String filePath) throws IOException {
        return IOUtils.toString(EhrExtractMapperTest.class.getResourceAsStream(filePath), StandardCharsets.UTF_8);
    }
}
