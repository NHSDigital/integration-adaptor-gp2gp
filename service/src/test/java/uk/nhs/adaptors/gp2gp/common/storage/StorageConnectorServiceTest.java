package uk.nhs.adaptors.gp2gp.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class StorageConnectorServiceTest {
    private static final String RESPONSE_STRING = "£$%^^&£$abc";
    private static final long EXPECTED_INPUT_LENGTH = 13;
    private static final InputStream EXPECTED_INPUT_STREAM = new ByteArrayInputStream(RESPONSE_STRING.getBytes());
    @Captor
    private ArgumentCaptor<InputStream> actualInputStream;
    @Captor
    private ArgumentCaptor<Long> actualInputLength;
    @Captor
    private ArgumentCaptor<String> actualFileName;
    @Mock
    private StorageDataWrapper storageDataWrapper;
    @Mock
    private StorageConnector storageConnector;
    @InjectMocks
    private StorageConnectorService storageConnectorService;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void When_ValidStorageDataWrapperIsPass_Expect_UploadStoragePramsHaveCorrectValues() throws IOException {
        var conversationId = "conversationId";
        when(storageDataWrapper.getConversationId()).thenReturn(conversationId);
        when(objectMapper.writeValueAsString(any())).thenReturn(RESPONSE_STRING);

        storageConnectorService.uploadWithMetadata(storageDataWrapper);
        Mockito.verify(storageConnector).uploadToStorage(actualInputStream.capture(),
            actualInputLength.capture(), actualFileName.capture());

        assertThat(actualFileName.getValue()).isEqualTo(conversationId + GPC_STRUCTURED_FILE_EXTENSION);
        assertThat(actualInputLength.getValue()).isEqualTo(EXPECTED_INPUT_LENGTH);
        assertThat(actualInputStream.getValue().readAllBytes()).isEqualTo(EXPECTED_INPUT_STREAM.readAllBytes());
    }
}
