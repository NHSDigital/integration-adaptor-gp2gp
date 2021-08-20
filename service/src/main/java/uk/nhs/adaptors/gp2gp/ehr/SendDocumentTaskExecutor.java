package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrDocumentTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendDocumentTaskExecutor implements TaskExecutor<SendDocumentTaskDefinition> {
    private static final int THRESHOLD_MINIMUM = 4;
    private static final String MESSAGE_ATTACHMENT_EXTENSION = ".messageattachment";

    private final StorageConnectorService storageConnectorService;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final MhsClient mhsClient;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final ObjectMapper objectMapper;
    private final DetectDocumentsSentService detectDocumentsSentService;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final EhrDocumentMapper ehrDocumentMapper;
    private final TimestampService timestampService;

    @Override
    public Class<SendDocumentTaskDefinition> getTaskType() {
        return SendDocumentTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(SendDocumentTaskDefinition taskDefinition) {
        LOGGER.info("SendDocument task was created, Sending EHR Document to GP");
        var storageDataWrapper = storageConnectorService.downloadFile(taskDefinition.getDocumentName());
        var mainMessageId = taskDefinition.getMessageId();
        var mainDocumentId = taskDefinition.getDocumentId();
        var requestDataToSend = new HashMap<String, String>();

        var outboundMessage = objectMapper.readValue(storageDataWrapper.getData(), OutboundMessage.class);
        if (outboundMessage.getAttachments().size() != 1) {
            throw new IllegalStateException("There should be exactly 1 attachment - the document binary");
        }

        var binary = outboundMessage.getAttachments().get(0).getPayload();
        if (isLargeAttachment(binary)) {
            var contentType = outboundMessage.getAttachments().get(0).getContentType();
            outboundMessage.getAttachments().clear(); // since it's a large message, chunks will be sent as external attachments
            outboundMessage.setExternalAttachments(new ArrayList<>());

            var chunks = chunkBinary(binary, gp2gpConfiguration.getLargeAttachmentThreshold());
            for (int i = 0; i < chunks.size(); i++) {
                var chunk = chunks.get(i);
                var messageId = randomIdGeneratorService.createNewId();
                var filename = mainDocumentId + "_" + i + MESSAGE_ATTACHMENT_EXTENSION;
                var chunkPayload = generateChunkPayload(taskDefinition, messageId, filename);
                var chunkedOutboundMessage = createChunkOutboundMessage(chunkPayload, chunk, contentType);
                requestDataToSend.put(randomIdGeneratorService.createNewId(), chunkedOutboundMessage);
                var externalAttachment = OutboundMessage.ExternalAttachment.builder()
                    .description(OutboundMessage.AttachmentDescription.builder()
                        .fileName(filename)
                        .contentType(contentType)
                        .compressed(false) //const
                        .largeAttachment(false) // const - chunks are not large attachments themself
                        .originalBase64(true) //const
                        .build()
                        .toString())
                    .messageId(messageId)
                    .build();
                outboundMessage.getExternalAttachments().add(externalAttachment);
            }

            requestDataToSend.put(mainMessageId, objectMapper.writeValueAsString(outboundMessage));
        } else {
            requestDataToSend.put(mainMessageId, storageDataWrapper.getData());
        }

        requestDataToSend.entrySet().stream()
            .map(kv -> mhsRequestBuilder
                .buildSendEhrExtractCommonRequest(
                    kv.getValue(),
                    taskDefinition.getConversationId(),
                    taskDefinition.getFromOdsCode(),
                    kv.getKey()))
            .forEach(mhsClient::sendMessageToMHS);

        var ehrExtractStatus = ehrExtractStatusService
            .updateEhrExtractStatusCommon(taskDefinition, new ArrayList<>(requestDataToSend.keySet()));

        detectDocumentsSentService.beginSendingPositiveAcknowledgement(ehrExtractStatus);
    }

    @SneakyThrows
    private String createChunkOutboundMessage(String chunkPayload, String chunk, String contentType) {
        var chunkOutboundMessage = OutboundMessage.builder()
            .payload(chunkPayload)
            .attachments(List.of(OutboundMessage.Attachment.builder()
                .contentType(contentType)
                .isBase64("true")
                .description("Attachment")
                .payload(chunk)
                .build()))
            .build();
        return objectMapper.writeValueAsString(chunkOutboundMessage);
    }

    private String generateChunkPayload(DocumentTaskDefinition taskDefinition, String messageId, String filename) {
        var templateParameters = EhrDocumentTemplateParameters.builder()
            .resourceCreated(DateFormatUtil.toHl7Format(timestampService.now()))
            .messageId(messageId)
            .accessDocumentId(filename)
            .fromAsid(taskDefinition.getFromAsid())
            .toAsid(taskDefinition.getToAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .pertinentPayloadId(randomIdGeneratorService.createNewId())
            .build();

        return ehrDocumentMapper.mapMhsPayloadTemplateToXml(templateParameters);
    }

    public static List<String> chunkBinary(String binary, int sizeThreshold) {
        if (sizeThreshold <= THRESHOLD_MINIMUM) {
            throw new IllegalArgumentException("SizeThreshold must be larger 4 to hold at least 1 UTF-16 character");
        }

        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < binary.length(); i++) {
            var c = binary.charAt(i);
            var chunkBytesSize = chunk.toString().getBytes(StandardCharsets.UTF_8).length;
            var charBytesSize = Character.toString(c).getBytes(StandardCharsets.UTF_8).length;
            if (chunkBytesSize + charBytesSize > sizeThreshold) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(c);
        }
        if (chunk.length() != 0) {
            chunks.add(chunk.toString());
        }

        return chunks;
    }

    private boolean isLargeAttachment(String binary) {
        var bytes = binary.getBytes(StandardCharsets.UTF_8);
        return bytes.length > gp2gpConfiguration.getLargeAttachmentThreshold();
    }
}
