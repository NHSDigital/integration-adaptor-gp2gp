package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PractitionerAgentPersonMapperTest {

    private static final String TEST_ID = "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73";
    private static final String PRACTITIONER_FILE_LOCATION = "/ehr/mapper/practitioner/";
    private static final String PRACTITIONER_ONLY_FILE_LOCATION = PRACTITIONER_FILE_LOCATION + "practitioner-only/";
    private static final String PRACTITIONER_ROLE_FILE_LOCATION = PRACTITIONER_FILE_LOCATION + "practitioner-role/";
    private static final String ORGANIZATION = PRACTITIONER_FILE_LOCATION + "organization.json";
    private static final String PRACTITIONER = PRACTITIONER_FILE_LOCATION + "practitioner.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private PractitionerAgentPersonMapper practitionerAgentPersonMapper;
    private MessageContext messageContext;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        OrganizationToAgentMapper organizationToAgentMapper = new OrganizationToAgentMapper(messageContext);
        practitionerAgentPersonMapper = new PractitionerAgentPersonMapper(messageContext, organizationToAgentMapper);
        fhirParseService = new FhirParseService();
    }

    @ParameterizedTest
    @MethodSource("readPractitionerRoleTests")
    public void When_MappingPractitionerToAgent_Expect_RepresentedAgentXml(String practitionerRoleJson, String outputXml)
        throws IOException {

        var jsonInputPractitionerRole = ResourceTestFileUtils.getFileContent(practitionerRoleJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        Practitioner practitioner = getPractitionerResource();
        PractitionerRole practitionerRole = fhirParseService.parseResource(jsonInputPractitionerRole, PractitionerRole.class);
        Organization organization = getOrganizationResource();

        var outputMessage = practitionerAgentPersonMapper
            .mapPractitionerToAgentPerson(practitioner, Optional.of(practitionerRole), Optional.of(organization));
        assertThat(outputMessage)
            .withFailMessage(TestArgumentsLoaderUtil.FAIL_MESSAGE, practitionerRoleJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("readPractitionerOnlyTests")
    public void When_MappingPractitionerOnlyToAgent_Expect_NoRepresentedOrganization(String inputJson, String outputXml)
        throws IOException {
        var jsonInputPractitioner = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        Practitioner practitioner = fhirParseService.parseResource(jsonInputPractitioner, Practitioner.class);
        var outputMessage = practitionerAgentPersonMapper
            .mapPractitionerToAgentPerson(practitioner, Optional.empty(), Optional.empty());
        assertThat(outputMessage)
            .withFailMessage(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    private static Stream<Arguments> readPractitionerOnlyTests() {
        return TestArgumentsLoaderUtil.readTestCases(PRACTITIONER_ONLY_FILE_LOCATION);
    }

    private static Stream<Arguments> readPractitionerRoleTests() {
        return TestArgumentsLoaderUtil.readTestCases(PRACTITIONER_ROLE_FILE_LOCATION);
    }

    private static Practitioner getPractitionerResource() throws IOException {
        String jsonPractitioner = ResourceTestFileUtils.getFileContent(PRACTITIONER);
        return new FhirParseService().parseResource(jsonPractitioner, Practitioner.class);
    }

    private static Organization getOrganizationResource() throws IOException {
        String jsonOrganization = ResourceTestFileUtils.getFileContent(ORGANIZATION);
        return new FhirParseService().parseResource(jsonOrganization, Organization.class);
    }
}
