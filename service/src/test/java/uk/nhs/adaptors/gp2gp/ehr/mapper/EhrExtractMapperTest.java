package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.mockito.Mockito.mock;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@ExtendWith(MockitoExtension.class)
public class EhrExtractMapperTest {
    private static final String OVERRIDE_NHS_NUMBER = "overrideNhsNumber";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private OrganizationToAgentMapper organizationToAgentMapper;
    @Mock
    private NonConsultationResourceMapper nonConsultationResourceMapper;
    @Mock
    private MessageContext messageContext;
    @InjectMocks
    private EhrExtractMapper ehrExtractMapper;

    @Test
    public void When_NhsOverrideNumberProvided_Expect_OverrideToBeUsed()  {
        ReflectionTestUtils.setField(ehrExtractMapper, OVERRIDE_NHS_NUMBER, "123");
        // mock agentDirectoryMapper to only produce expected output if "override" nhs number used
        var taskDef = GetGpcStructuredTaskDefinition.builder().build();
        var bundle = mock(Bundle.class);
        var parameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(taskDef, bundle);
        // assert expected agentDirectoryMapper output
    }

    @Test
    public void When_NhsOverrideNumberIsBlank_Expect_ActualNhsNumberIsUsed()  {

    }
}
