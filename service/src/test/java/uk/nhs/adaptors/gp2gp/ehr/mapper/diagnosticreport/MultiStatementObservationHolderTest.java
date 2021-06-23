package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

@ExtendWith(MockitoExtension.class)
public class MultiStatementObservationHolderTest {
    private static final String MAPPED_ID = "mapped-id";
    private static final String RANDOM_ID = "random-id";

    @Mock
    private Observation observation;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private IdMapper idMapper;
    @Mock
    private MessageContext messageContext;
    private MultiStatementObservationHolder multiStatementObservationHolder;

    @BeforeEach
    public void before() {
        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(idMapper.getOrNew(eq(ResourceType.Observation), any(IdType.class)))
            .thenReturn(MAPPED_ID);
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(RANDOM_ID);
        lenient().when(observation.getResourceType()).thenReturn(ResourceType.Observation);
        when(observation.getIdElement()).thenReturn(new IdType());
        multiStatementObservationHolder = new MultiStatementObservationHolder(
            observation, messageContext, randomIdGeneratorService
        );
    }

    @Test
    public void When_NextHl7InstanceIdentifier_Expect_FirstIsMappedSecondIsRandomAndVerifies() {
        assertThat(multiStatementObservationHolder.nextHl7InstanceIdentifier())
            .isEqualTo(MAPPED_ID);
        assertThatCode(() -> multiStatementObservationHolder.verifyObservationWasMapped())
            .doesNotThrowAnyException();

        assertThat(multiStatementObservationHolder.nextHl7InstanceIdentifier())
            .isEqualTo(RANDOM_ID);
        assertThatCode(() -> multiStatementObservationHolder.verifyObservationWasMapped())
            .doesNotThrowAnyException();
    }

    @Test
    public void When_GetObservation_Expect_HeldObservationIsReturned() {
        assertThat(multiStatementObservationHolder.getObservation())
            .isSameAs(observation);
    }

    @Test
    public void When_NoInstanceIdentifierProduced_Expect_VerifyThrowsException() {
        assertThatCode(() -> multiStatementObservationHolder.verifyObservationWasMapped())
            .isInstanceOf(EhrMapperException.class)
            .hasMessageContaining("was not mapped to a statement");
    }

}
