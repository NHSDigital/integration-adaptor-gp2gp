package uk.nhs.adaptors.gp2gp.common.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class StorageConnectorServiceTest {
    private static final long EXPECTED_STREAM_LENGTH = 8;
    @Mock
    private StorageConnector storageConnector;
    @InjectMocks
    private StorageConnectorService storageConnectorService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StorageDataWrapper anyStorageDataWrapper;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamArgumentCaptor;

    @Test
    @SneakyThrows
    public void When_ValidStorageDataWrapperIsPass_Expect_UploadStoragePramsHaveCorrectValues() {
        when(objectMapper.writeValueAsString(anyStorageDataWrapper)).thenReturn("response");
        when(anyStorageDataWrapper.getConversationId()).thenReturn("UUID");
        storageConnectorService.uploadWithMetadata(anyStorageDataWrapper);
        verify(storageConnector).uploadToStorage(
            inputStreamArgumentCaptor.capture(),
            eq(EXPECTED_STREAM_LENGTH),
            eq("UUID" + GPC_STRUCTURED_FILE_EXTENSION));
        var actualContent = new String(inputStreamArgumentCaptor.getValue().readAllBytes(), UTF_8);
        assertThat(actualContent).isEqualTo("response");
    }
}
