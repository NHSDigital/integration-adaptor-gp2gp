package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.utils.BinaryUtils.getBytesLengthOfString;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendDocumentTaskExecutor implements TaskExecutor<SendDocumentTaskDefinition> {
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

    @Override
    public Class<SendDocumentTaskDefinition> getTaskType() {
        return SendDocumentTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(SendDocumentTaskDefinition taskDefinition) {
        var storageDataWrapper = storageConnectorService.downloadFile(taskDefinition.getDocumentName());
        var mainMessageId = taskDefinition.getMessageId();
        var requestDataToSend = new ArrayList<Pair<String, String>>();

        var outboundMessage = objectMapper.readValue(storageDataWrapper.getData(), OutboundMessage.class);
        if (outboundMessage.getAttachments().size() != 1) {
            throw new IllegalStateException("There should be exactly 1 attachment - the document binary");
        }

        var binary = outboundMessage.getAttachments().get(0).getPayload();
        LOGGER.debug("Attachment size=" + getBytesLengthOfString(binary) + " content-type=" + taskDefinition.getDocumentContentType());
        if (isLargeAttachment(binary)) {
            outboundMessage.getAttachments().clear(); // since it's a large message, chunks will be sent as external attachments
            outboundMessage.setExternalAttachments(new ArrayList<>());

            var chunks = chunkBinary(binary, gp2gpConfiguration.getLargeAttachmentThreshold());
            LOGGER.debug("Attachment split into {} chunks", chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                LOGGER.debug("Handling chunk {}", i);
                var chunk = chunks.get(i);
                var messageId = randomIdGeneratorService.createNewId();
                var filename = mainMessageId + "_" + i + MESSAGE_ATTACHMENT_EXTENSION;
                var chunkPayload = ehrDocumentMapper.generateMhsPayload(taskDefinition, messageId, filename);

                var chunkedOutboundMessage = createChunkOutboundMessage(chunkPayload, chunk, MimeTypes.OCTET_STREAM);

                requestDataToSend.add(Pair.of(messageId, chunkedOutboundMessage));
                var externalAttachment = OutboundMessage.ExternalAttachment.builder()
                    .description(OutboundMessage.AttachmentDescription.builder()
                        .length(getBytesLengthOfString(chunk)) //calculate size for chunk
                        .fileName(filename)
                        .contentType(taskDefinition.getDocumentContentType())
                        .compressed(false) //const
                        .largeAttachment(false) // const - chunks are not large attachments themself
                        .originalBase64(true) //const
                        .build()
                        .toString())
                    .messageId(messageId)
                    .build();
                outboundMessage.getExternalAttachments().add(externalAttachment);
            }
            var outboundMessageAsString = objectMapper.writeValueAsString(outboundMessage);
            requestDataToSend.add(0, Pair.of(mainMessageId, outboundMessageAsString));
        } else {
            requestDataToSend.add(Pair.of(mainMessageId, storageDataWrapper.getData()));
        }

        requestDataToSend.stream()
            .map(pair -> mhsRequestBuilder
                .buildSendEhrExtractCommonRequest(
                    pair.getSecond(),
                    taskDefinition.getConversationId(),
                    taskDefinition.getFromOdsCode(),
                    pair.getFirst()))
            .forEach(mhsClient::sendMessageToMHS);

        EhrExtractStatus ehrExtractStatus;

        var sentIds = requestDataToSend.stream()
            .map(Pair::getFirst)
            .collect(Collectors.toList());

        ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusCommonForDocuments(taskDefinition, sentIds);

        LOGGER.info("Executing beginSendingPositiveAcknowledgement");
        detectDocumentsSentService.beginSendingPositiveAcknowledgement(ehrExtractStatus);
    }

    @SneakyThrows
    private String createChunkOutboundMessage(String chunkPayload, String chunk, String contentType) {
        var chunkOutboundMessage = OutboundMessage.builder()
            .payload(chunkPayload)
            .attachments(List.of(OutboundMessage.Attachment.builder()
                .contentType(contentType)
                .isBase64(Boolean.TRUE)
                .description("Attachment")
                .payload(chunk)
                .build()))
            .build();
        return objectMapper.writeValueAsString(chunkOutboundMessage);
    }

    public static List<String> chunkBinary(String str, int sizeThreshold) {
        // assuming that the "str" is always in base64 so 1 char == 1 byte
        var chunksCount = (int) Math.ceil((double) str.length() / sizeThreshold);
        var chunks = new ArrayList<String>(chunksCount);

        for (int i = 0; i < chunksCount; i++) {
            chunks.add(str.substring(i * sizeThreshold, Math.min((i + 1) * sizeThreshold, str.length())));
        }

        return chunks;
    }

    private boolean isLargeAttachment(String binary) {
        return getBytesLengthOfString(binary) > gp2gpConfiguration.getLargeAttachmentThreshold();
    }
}
