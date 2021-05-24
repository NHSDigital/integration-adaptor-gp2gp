package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
    private static final String NHS_NUMBER = "1234567890";
    private static final String OVERRIDE_NHS_NUMBER = "overrideNhsNumber";
    private static final String OVERRIDE_NHS_NUMBER_VALUE = "123123123";

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
    @Mock
    private AgentDirectoryMapper agentDirectoryMapper;
    @Mock
    private EncounterMapper encounterMapper;
    @Mock
    private EhrFolderEffectiveTime ehrFolderEffectiveTime;
    @InjectMocks
    private EhrExtractMapper ehrExtractMapper;

    @Test
    public void When_NhsOverrideNumberProvided_Expect_OverrideToBeUsed()  {
        ReflectionTestUtils.setField(ehrExtractMapper, OVERRIDE_NHS_NUMBER, OVERRIDE_NHS_NUMBER_VALUE);
        when(agentDirectoryMapper.mapEHRFolderToAgentDirectory(any(Bundle.class), eq(OVERRIDE_NHS_NUMBER_VALUE)))
            .thenReturn(OVERRIDE_NHS_NUMBER_VALUE);
        when(timestampService.now()).thenReturn(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        when(messageContext.getEffectiveTime()).thenReturn(ehrFolderEffectiveTime);

        var taskDef = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(NHS_NUMBER)
            .build();
        var bundle = mock(Bundle.class);
        var parameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(taskDef, bundle);
        assertThat(parameters.getAgentDirectory()).isEqualTo(OVERRIDE_NHS_NUMBER_VALUE);
    }

    @Test
    public void When_NhsOverrideNumberIsBlank_Expect_ActualNhsNumberIsUsed()  {
        when(agentDirectoryMapper.mapEHRFolderToAgentDirectory(any(Bundle.class), eq(NHS_NUMBER)))
            .thenReturn(NHS_NUMBER);
        when(timestampService.now()).thenReturn(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        when(messageContext.getEffectiveTime()).thenReturn(ehrFolderEffectiveTime);

        var taskDef = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(NHS_NUMBER)
            .build();
        var bundle = mock(Bundle.class);
        var parameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(taskDef, bundle);
        assertThat(parameters.getAgentDirectory()).isEqualTo(NHS_NUMBER);
    }
}
