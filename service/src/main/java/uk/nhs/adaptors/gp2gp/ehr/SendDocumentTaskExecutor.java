package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.utils.BinaryUtils.getBytesLengthOfString;

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
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
        var storageDataWrapper = storageConnectorService.downloadFile(taskDefinition.getDocumentName());
        var mainMessageId = taskDefinition.getMessageId();
        var mainDocumentId = taskDefinition.getDocumentId();
        var requestDataToSend = new HashMap<String, String>();

        var outboundMessage = objectMapper.readValue(storageDataWrapper.getData(), OutboundMessage.class);
        if (outboundMessage.getAttachments().size() != 1) {
            throw new IllegalStateException("There should be exactly 1 attachment - the document binary");
        }

        var binary = outboundMessage.getAttachments().get(0).getPayload();
        LOGGER.info("Attachment size=" + getBytesLengthOfString(binary));
        if (isLargeAttachment(binary)) {
            LOGGER.info("Attachment is large");
            var contentType = outboundMessage.getAttachments().get(0).getContentType();
            outboundMessage.getAttachments().clear(); // since it's a large message, chunks will be sent as external attachments
            outboundMessage.setExternalAttachments(new ArrayList<>());

            var chunks = chunkBinary(binary, gp2gpConfiguration.getLargeAttachmentThreshold());
            LOGGER.info("Attachment split into {} chunks", chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                LOGGER.info("Handling chunk {}", i);
                var chunk = chunks.get(i);
                var messageId = randomIdGeneratorService.createNewId();
                var filename = mainDocumentId + "_" + i + MESSAGE_ATTACHMENT_EXTENSION;
                LOGGER.info("Building chunk payload");
                var chunkPayload = generateChunkPayload(taskDefinition, messageId, filename);
                LOGGER.info("Building chunk outbound message");
                var chunkedOutboundMessage = createChunkOutboundMessage(chunkPayload, chunk, contentType);
                var id = randomIdGeneratorService.createNewId();
                LOGGER.info("Adding requestDataToSend key={} value={}", id, chunkedOutboundMessage);
                requestDataToSend.put(id, chunkedOutboundMessage);
                LOGGER.info("Building external attachment");
                var externalAttachment = OutboundMessage.ExternalAttachment.builder()
                    .description(OutboundMessage.AttachmentDescription.builder()
                        .length(getBytesLengthOfString(chunk)) //calculate size for chunk
                        .fileName(filename)
                        .contentType(contentType)
                        .compressed(false) //const
                        .largeAttachment(false) // const - chunks are not large attachments themself
                        .originalBase64(true) //const
                        .build()
                        .toString())
                    .messageId(messageId)
                    .build();
                LOGGER.info("Adding external attachment");
                outboundMessage.getExternalAttachments().add(externalAttachment);
            }
            var outboundMessageAsString = objectMapper.writeValueAsString(outboundMessage);
            LOGGER.info("Finished handling chunks. Adding requestDataToSend key={} value={}", mainMessageId, outboundMessageAsString);
            requestDataToSend.put(mainMessageId, outboundMessageAsString);
        } else {
            requestDataToSend.put(mainMessageId, storageDataWrapper.getData());
        }

        LOGGER.info("Sending all requestDataToSend");
        requestDataToSend.entrySet().stream()
            .map(kv -> {
                LOGGER.info("Building request");
                return mhsRequestBuilder
                    .buildSendEhrExtractCommonRequest(
                        kv.getValue(),
                        taskDefinition.getConversationId(),
                        taskDefinition.getFromOdsCode(),
                        kv.getKey());
            })
            .forEach(request -> {
                LOGGER.info("Sending request");
                mhsClient.sendMessageToMHS(request);
            });

        EhrExtractStatus ehrExtractStatus;

        if (taskDefinition.isExternalEhrExtract()) {
            LOGGER.info("Is external ehr extract");
            ehrExtractStatus = ehrExtractStatusService
                .updateEhrExtractStatusCommonForExternalEhrExtract(taskDefinition, new ArrayList<>(requestDataToSend.keySet()));
        } else {
            ehrExtractStatus = ehrExtractStatusService
                .updateEhrExtractStatusCommonForDocuments(taskDefinition, new ArrayList<>(requestDataToSend.keySet()));
        }

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

        LOGGER.info("Getting bytes");
        var bytes = binary.getBytes(StandardCharsets.UTF_8);
        var chunksCount = (int) Math.ceil((double) bytes.length / sizeThreshold) + 1;
        LOGGER.info("Chunking string with lenght={} bytes_count={} into {} chunks", binary.length(), bytes.length, chunksCount);
        var chunks = new byte[chunksCount][];

        var chunk = new byte[sizeThreshold];
        var chunkIndex = 0;
        var chunkNumber = 0;

        for (byte c : bytes) {
            if (chunkIndex >= sizeThreshold) {
                LOGGER.info("Adding chunk to chunks");
                chunks[chunkNumber] = chunk;
                chunkNumber++;
                chunk = new byte[sizeThreshold];
                chunkIndex = 0;
            }

            chunk[chunkIndex] = c;
            chunkIndex++;
        }
        if (chunkIndex != 0) {
            LOGGER.info("Adding last chunk");
            chunks[chunkNumber] = Arrays.copyOf(chunk, chunkIndex);
        }

//        StringBuilder chunk = new StringBuilder();
//        for (int i = 0; i < bytes.length; i++) {
//            var c = bytes[i];
//            var chunkBytesSize = chunk.toString().getBytes(StandardCharsets.UTF_8).length;
//            var charBytesSize = Character.toString(c).getBytes(StandardCharsets.UTF_8).length;
//            if (chunkBytesSize + charBytesSize > sizeThreshold) {
//                LOGGER.info("Adding chunk number={} size={}", chunks.size() + 1, chunkBytesSize);
//                chunks.add(chunk.toString());
//                chunk = new StringBuilder();
//            }
//            chunk.append(c);
//        }
//        if (chunk.length() != 0) {
//            LOGGER.info("Adding last chunk number={}", chunks.size() + 1);
//            chunks.add(chunk.toString());
//        }

        LOGGER.info("Converting chunks into strings");
        var result = Arrays.stream(chunks).map(byteArray -> new String(byteArray, StandardCharsets.UTF_8)).collect(Collectors.toList());
        LOGGER.info("Returning chunks as strings");
        return result;
    }

    private boolean isLargeAttachment(String binary) {
        return getBytesLengthOfString(binary) > gp2gpConfiguration.getLargeAttachmentThreshold();
    }
}
