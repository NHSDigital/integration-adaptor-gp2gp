package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.RandomIdGeneratorServiceStub;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorFactory;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorOptions;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SendDocumentTaskExecutorTest {
    private static final int SIZE_THRESHOLD_FOUR = 4;
    @Mock private EhrExtractStatusService ehrExtractStatusService;
    @Mock private DetectDocumentsSentService detectDocumentsSentService;
    @Mock private MhsClient mhsClient;
    @Mock private MhsRequestBuilder mhsRequestBuilder;
    private SendDocumentTaskExecutor sendDocumentTaskExecutor;
    private StorageConnector storageConnector;
    private Gp2gpConfiguration gp2gpConfiguration;

    @ParameterizedTest
    @MethodSource("chunkTestData")
    void When_ChunkingString_Expect_StringIsProperlySplit(String input, int sizeThreshold, List<String> output) {
        var result = SendDocumentTaskExecutor.chunkBinary(input, sizeThreshold);
        assertThat(result).containsExactlyElementsOf(output);
    }

    @SneakyThrows
    @Test
    public void When_DocumentNeedsToBeSplitIntoFiveChunks_Expect_FiveMhsRequestsWithAttachmentsOfContentTypeOctetStream() {
        final int SIZE_OF_EACH_CHUNK = 1;
        final int NUMBER_OF_CHUNKS = 5;
        final String storageFileName = "large_file_which_will_be_split.txt";

        // Arrange
        this.gp2gpConfiguration.setLargeAttachmentThreshold(SIZE_OF_EACH_CHUNK);
        uploadDocumentToStorageWrapperWithPayloadSize(SIZE_OF_EACH_CHUNK * NUMBER_OF_CHUNKS, storageFileName, "should-not-be-used");

        // Act
        this.sendDocumentTaskExecutor.execute(
            SendDocumentTaskDefinition.builder()
                .documentName(storageFileName)
                .messageId("88")
                .fromOdsCode("RANDOM-ODS")
                .conversationId("RANDOM-ID")
                .documentContentType("should-be-used")
                .build()
        );

        // Assert
        verify(mhsRequestBuilder, times(NUMBER_OF_CHUNKS)).buildSendEhrExtractCommonRequest(
            argThat(mhsRequestBodyContainsAttachmentWithContentType(MimeTypes.OCTET_STREAM)),
            eq("RANDOM-ID"),
            eq("RANDOM-ODS"),
            any()
        );
    }

    @DisplayName("When_DocumentNeedsToBeSplitInto5Chunks_Expect_"
            + "MhsMessageWith5ExternalAttachmentWithDescriptionContentTypeHeaderFromTaskDefinition")
    @Test
    public void When_DocumentNeedsToBeSplitInto5Chunks_Expect_MhsMessageWith5ExternalAttachmentCorrectlySet() {
        final int SIZE_OF_EACH_CHUNK = 1;
        final int NUMBER_OF_CHUNKS = 5;
        final String storageFileName = "large_file_which_will_be_split.txt";

        // Arrange
        this.gp2gpConfiguration.setLargeAttachmentThreshold(SIZE_OF_EACH_CHUNK);
        uploadDocumentToStorageWrapperWithPayloadSize(SIZE_OF_EACH_CHUNK * NUMBER_OF_CHUNKS, storageFileName, "should-not-be-used");

        // Act
        this.sendDocumentTaskExecutor.execute(
                SendDocumentTaskDefinition.builder()
                        .documentName(storageFileName)
                        .messageId("88")
                        .fromOdsCode("RANDOM-ODS")
                        .conversationId("RANDOM-ID")
                        .documentContentType("should-be-used")
                        .build()
        );

        // Assert
        verify(mhsRequestBuilder, times(1)).buildSendEhrExtractCommonRequest(
                argThat(mhsRequestBodyWithAnExternalAttachmentForEachChunkWithContentType(NUMBER_OF_CHUNKS, "should-be-used")),
                eq("RANDOM-ID"),
                eq("RANDOM-ODS"),
                any()
        );
    }

    @NotNull
    private static ArgumentMatcher<String> mhsRequestBodyContainsAttachmentWithContentType(String contentType) {
        return mhsRequestBody -> {
            ObjectMapper objectMapper = new ObjectMapper();
            OutboundMessage outboundMessage;
            try {
                outboundMessage = objectMapper.readValue(mhsRequestBody, OutboundMessage.class);
            } catch (JsonProcessingException e) {
                return false;
            }
            if (outboundMessage.getAttachments().isEmpty()) {
                return false;
            }
            return Objects.equals(outboundMessage.getAttachments().get(0).getContentType(), contentType);
        };
    }

    @NotNull
    private static ArgumentMatcher<String>
        mhsRequestBodyWithAnExternalAttachmentForEachChunkWithContentType(int numberOfChunks, String contentType) {

        return mhsRequestBody -> {
            ObjectMapper objectMapper = new ObjectMapper();
            OutboundMessage outboundMessage;
            try {
                outboundMessage = objectMapper.readValue(mhsRequestBody, OutboundMessage.class);
            } catch (JsonProcessingException e) {
                return false;
            }
            if (outboundMessage.getExternalAttachments() == null || outboundMessage.getExternalAttachments().size() != numberOfChunks) {
                return false;
            }
            return outboundMessage.getExternalAttachments().stream().allMatch(
                    externalAttachment -> externalAttachment.getDescription().contains("ContentType=" + contentType)
            );
        };
    }

    private void uploadDocumentToStorageWrapperWithPayloadSize(int payloadSize, String storageFileName, String contentType) {
        byte[] storageDataWrapper = generateStorageDataWrapper(payloadSize, contentType);
        this.storageConnector.uploadToStorage(
            new ByteArrayInputStream(storageDataWrapper),
            storageDataWrapper.length,
                storageFileName
        );
    }

    @NotNull
    private static byte[] generateStorageDataWrapper(int sizeOfPayload, String contentType) {
        String payload = "a".repeat(sizeOfPayload);
        String attachment = "{\"content_type\":\"" + contentType + "\",\"is_base64\":false"
                + ",\"description\":\"\",\"payload\":\"" + payload + "\"}";
        String outboundMessage = "{\"payload\": \"\", \"attachments\": [" + attachment + "], \"external_attachments\": []}";
        String encodedData = outboundMessage.replace("\"", "\\\""); // Poor persons JSON encode
        String storageDataWrapper = "{\"type\": \"\", \"conversationId\": \"\", \"taskId\": \"\", \"data\": \"" + encodedData + "\"}";
        return storageDataWrapper.getBytes();
    }

    static Stream<Arguments> chunkTestData() {
        return Stream.of(
            Arguments.of("QWER1234", SIZE_THRESHOLD_FOUR, List.of("QWER", "1234")),
            Arguments.of("QWER", SIZE_THRESHOLD_FOUR, List.of("QWER")),
            Arguments.of("QWE", SIZE_THRESHOLD_FOUR, List.of("QWE")),
            Arguments.of("QWER12", SIZE_THRESHOLD_FOUR, List.of("QWER", "12"))
        );
    }

    @BeforeEach
    public void setup() {
        this.storageConnector = createLocalStorageConnector();
        gp2gpConfiguration = new Gp2gpConfiguration();
        this.sendDocumentTaskExecutor = new SendDocumentTaskExecutor(
                new StorageConnectorService(storageConnector, new ObjectMapper()),
                this.mhsRequestBuilder,
                this.mhsClient,
                new RandomIdGeneratorServiceStub(),
                this.ehrExtractStatusService,
                new ObjectMapper(),
                this.detectDocumentsSentService,
                gp2gpConfiguration,
                new EhrDocumentMapper(new TimestampService(), new RandomIdGeneratorServiceStub())
        );
    }

    @Nullable
    private static StorageConnector createLocalStorageConnector() {
        StorageConnectorFactory storageConnectorFactory = new StorageConnectorFactory();
        StorageConnectorConfiguration configuration = new StorageConnectorConfiguration();
        configuration.setType(StorageConnectorOptions.LOCALMOCK.getStringValue());
        storageConnectorFactory.setConfiguration(configuration);
        return storageConnectorFactory.getObject();
    }
}
