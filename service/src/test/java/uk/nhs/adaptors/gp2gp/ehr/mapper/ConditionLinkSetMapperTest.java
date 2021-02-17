package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.TimeZone;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConditionLinkSetMapperTest {

    private static final String TEST_ID = "C93659E1-1107-441C-BE25-C5EF4B7831D1";
    private static final String CONDITION_FILE_LOCATIONS = "/ehr/mapper/condition/";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM = CONDITION_FILE_LOCATIONS
        + "example-problem-header-condition.json";
    private static final String INPUT_JSON_BUNDLE = CONDITION_FILE_LOCATIONS
        + "fhir-bundle.json";

//    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private MessageContext messageContext;
    private ConditionLinkSetMapper conditionLinkSetMapper;
    private FhirParseService fhirParseService;
    private Bundle bundle;

    @BeforeEach
    public void setUp() throws IOException {
        //when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        randomIdGeneratorService = new RandomIdGeneratorService();
        fhirParseService = new FhirParseService();
        messageContext = new MessageContext(randomIdGeneratorService);
        conditionLinkSetMapper = new ConditionLinkSetMapper(messageContext);
        var jsonBundle = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = fhirParseService.parseResource(jsonBundle, Bundle.class);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @AfterAll
    public static void deinitialize() {
        TimeZone.setDefault(null);
    }

    @Test
    public void When_MappingParsedConditionWithRealProblem_Expect_LinkSetXml() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_ACTUAL_PROBLEM);
        Condition condition = fhirParseService.parseResource(jsonInput, Condition.class);

        String outputMessage = conditionLinkSetMapper.mapConditionToLinkSet(condition, bundle, false);
    }
}
