package uk.nhs.adaptors.gp2gp.common.configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;

import static com.mongodb.assertions.Assertions.assertFalse;
import static com.mongodb.assertions.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObjectMapperPostProcessorConfigTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EhrExtractStatusService ehrExtractStatusService;

    @Mock
    private JsonFactory mockJsonFactory;

    @InjectMocks
    private ObjectMapperPostProcessorConfig objectMapperPostProcessorConfig;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void objectMapperPostProcessSetsMaxDataProcessingLimitTest() {

        when(objectMapper.getFactory()).thenReturn(mockJsonFactory);

        var objectMapperWithoutDataLimit = objectMapperPostProcessorConfig.postProcessAfterInitialization(objectMapper, "objectMapper");

        assertTrue(objectMapperWithoutDataLimit instanceof ObjectMapper);
        verify(mockJsonFactory, times(1))
            .setStreamReadConstraints(argThat(constraints -> constraints.getMaxStringLength() == Integer.MAX_VALUE));
    }

    @Test
    public void limitInObjectMapperPostProcessSetOnlyForObjectMapperTest() {

        var objectMapperWithoutDataLimit
            = objectMapperPostProcessorConfig.postProcessAfterInitialization(ehrExtractStatusService, "service");

        assertFalse(objectMapperWithoutDataLimit instanceof ObjectMapper);
        verify(mockJsonFactory, times(0))
            .setStreamReadConstraints(argThat(constraints -> constraints.getMaxStringLength() == Integer.MAX_VALUE));
    }

}