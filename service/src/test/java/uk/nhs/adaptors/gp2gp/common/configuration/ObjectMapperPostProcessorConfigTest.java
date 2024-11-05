package uk.nhs.adaptors.gp2gp.common.configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObjectMapperPostProcessorConfigTest {

    @Mock
    private ObjectMapper objectMapper;

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

        objectMapperPostProcessorConfig.postProcessAfterInitialization(objectMapper, "objectMapper");

        verify(mockJsonFactory, times(1))
            .setStreamReadConstraints(argThat(constraints -> constraints.getMaxStringLength() == Integer.MAX_VALUE));
    }
}