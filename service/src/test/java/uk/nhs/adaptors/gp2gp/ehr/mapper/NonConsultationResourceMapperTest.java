package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class NonConsultationResourceMapperTest {
    private static final String FILES_DIRECTORY = "/ehr/mapper/non_consultation_bundles/";
    private static final String IMMUNIZATION_XML = FILES_DIRECTORY + "immunization.xml";
    private static final String IMMUNIZATION_BUNDLE = FILES_DIRECTORY + "immunization-bundle.json";
    private static final String EXPECTED_IMMUNIZATION_OUTPUT = FILES_DIRECTORY + "expected-immunization-output.xml";
    private static final String ALLERGY_INTOLERANCE_XML = FILES_DIRECTORY + "allergy-intolerance.xml";
    private static final String ALLERGY_INTOLERANCE_BUNDLE = FILES_DIRECTORY + "allergy-intolerance-bundle.json";
    private static final String EXPECTED_ALLERGY_INTOLERANCE_OUTPUT = FILES_DIRECTORY + "expected-allergy-intolerance-output.xml";
    private static final String REFERRAL_REQUEST_XML = FILES_DIRECTORY + "referral-request.xml";
    private static final String REFERRAL_REQUEST_BUNDLE = FILES_DIRECTORY + "referral-request-bundle.json";
    private static final String EXPECTED_REFERRAL_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-referral-request-output.xml";
    private static final String MEDICATION_REQUEST_XML = FILES_DIRECTORY + "medication-request.xml";
    private static final String MEDICATION_REQUEST_BUNDLE = FILES_DIRECTORY + "medication-request-bundle.json";
    private static final String EXPECTED_MEDICATION_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-medication-request-output.xml";
    private static final String CONDITION_XML = FILES_DIRECTORY + "condition.xml";
    private static final String CONDITION_BUNDLE = FILES_DIRECTORY + "condition-bundle.json";
    private static final String EXPECTED_CONDITION_OUTPUT = FILES_DIRECTORY + "expected-condition-output.xml";
    private static final String PROCEDURE_REQUEST_XML = FILES_DIRECTORY + "procedure-request.xml";
    private static final String PROCEDURE_REQUEST_BUNDLE = FILES_DIRECTORY + "procedure-request-bundle.json";
    private static final String EXPECTED_PROCEDURE_REQUEST_OUTPUT = FILES_DIRECTORY + "expected-procedure-request-output.xml";

    private NonConsultationResourceMapper nonConsultationResourceMapper;
    private MessageContext messageContext;
    private FhirParseService fhirParseService;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private EncounterComponentsMapper encounterComponentsMapper;

    @BeforeEach
    public void setUp() {
        messageContext = new MessageContext(randomIdGeneratorService);
        fhirParseService = new FhirParseService();
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    public void When_TransformingResourceToEhrComp_Expect_CorrectValuesToBeExtracted(String returnXml, String inputBundle, String output) throws IOException {
        setupMock(encounterComponentsMapper,
            ResourceTestFileUtils.getFileContent(returnXml),
            "123"
        );
        String bundle = ResourceTestFileUtils.getFileContent(inputBundle);
        String expectedOutput = ResourceTestFileUtils.getFileContent(output);
        Bundle parsedBundle = fhirParseService.parseResource(bundle, Bundle.class);

        var translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle).get(0);
        System.out.println(translatedOutput);
        assertThat(translatedOutput).isEqualTo(expectedOutput);
    }

    private static Stream<Arguments> testArgs() {
        return Stream.of(
            Arguments.of(ALLERGY_INTOLERANCE_XML, ALLERGY_INTOLERANCE_BUNDLE, EXPECTED_ALLERGY_INTOLERANCE_OUTPUT),
            Arguments.of(IMMUNIZATION_XML, IMMUNIZATION_BUNDLE, EXPECTED_IMMUNIZATION_OUTPUT),
            Arguments.of(REFERRAL_REQUEST_XML, REFERRAL_REQUEST_BUNDLE, EXPECTED_REFERRAL_REQUEST_OUTPUT),
            Arguments.of(MEDICATION_REQUEST_XML, MEDICATION_REQUEST_BUNDLE, EXPECTED_MEDICATION_REQUEST_OUTPUT),
            Arguments.of(CONDITION_XML, CONDITION_BUNDLE, EXPECTED_CONDITION_OUTPUT),
            Arguments.of(PROCEDURE_REQUEST_XML, PROCEDURE_REQUEST_BUNDLE, EXPECTED_PROCEDURE_REQUEST_OUTPUT)
        );
    }

    private void setupMock(EncounterComponentsMapper encounterComponentsMapper, String returnString, String uuid) {
        when(encounterComponentsMapper.mapResourceToComponent(any(Resource.class))).thenReturn(returnString);
        when(randomIdGeneratorService.createNewId()).thenReturn(uuid);
        nonConsultationResourceMapper = new NonConsultationResourceMapper(messageContext, randomIdGeneratorService,
            encounterComponentsMapper);
    }

}
