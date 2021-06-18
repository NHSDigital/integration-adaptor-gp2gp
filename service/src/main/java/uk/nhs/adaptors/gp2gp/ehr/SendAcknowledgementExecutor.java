package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.Mustache;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendAcknowledgementExecutor implements TaskExecutor<SendAcknowledgementTaskDefinition> {
    private static final Mustache ACKNOWLEDGEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("outbound_message_positive_acknowledgement.mustache");
    private static final Mustache NEGATIVE_ACK_TEMPLATE = TemplateUtils.loadTemplate("outbound_message_negative_acknowledgement.mustache");

    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final ObjectMapper objectMapper;
    private final TimestampService timestampService;
    private final RandomIdGeneratorService randomIdGeneratorService;

    @Override
    public Class<SendAcknowledgementTaskDefinition> getTaskType() {
        return SendAcknowledgementTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(SendAcknowledgementTaskDefinition sendAcknowledgementTaskDefinition) {
        LOGGER.info("Sending application acknowledgement from the adaptor to the requesting system");
        var ackMessageId = randomIdGeneratorService.createNewId();

        var sendAckTemplateParams = SendAckTemplateParameters.builder()
            .requestId(ackMessageId)
            .creationTime(DateFormatUtil.toHl7Format(timestampService.now()))
            .messageId(sendAcknowledgementTaskDefinition.getEhrRequestMessageId())
            .fromAsid(sendAcknowledgementTaskDefinition.getFromAsid())
            .toAsid(sendAcknowledgementTaskDefinition.getToAsid())
            .typeCode(sendAcknowledgementTaskDefinition.getTypeCode())
            .reasonCode(sendAcknowledgementTaskDefinition.getReasonCode())
            .reasonMessage(sendAcknowledgementTaskDefinition.getDetail())
            .build();

        String requestBody = buildRequestBody(sendAckTemplateParams, sendAcknowledgementTaskDefinition.isNack());

        sendToMHS(sendAcknowledgementTaskDefinition, ackMessageId, requestBody);

        updateEhrExtractStatus(sendAcknowledgementTaskDefinition, ackMessageId);
    }

    private void sendToMHS(SendAcknowledgementTaskDefinition taskDefinition, String ackMessageId, String requestBody) {
        LOGGER.info("Sending ACK message to MHS. ACK message Id: {} Conversation id: {} EhrRequest id {} ", ackMessageId,
            taskDefinition.getConversationId(), taskDefinition.getEhrRequestMessageId());

        var request = mhsRequestBuilder.buildSendAcknowledgement(
            requestBody, taskDefinition.getFromOdsCode(), taskDefinition.getConversationId(), ackMessageId);

        mhsClient.sendMessageToMHS(request);
    }

    private String buildRequestBody(SendAckTemplateParameters templateParams, boolean isNack) throws JsonProcessingException {
        var template = isNack ? NEGATIVE_ACK_TEMPLATE : ACKNOWLEDGEMENT_TEMPLATE;

        var acknowledgementRequestBody = TemplateUtils.fillTemplate(template, templateParams);
        var outboundMessage = OutboundMessage.builder().payload(acknowledgementRequestBody).build();

        return objectMapper.writeValueAsString(outboundMessage);
    }

    private void updateEhrExtractStatus(SendAcknowledgementTaskDefinition taskDefinition, String ackMessageId) {
        LOGGER.info("Updating EhrExtractStatus with ACK message Id: {} Conversation id: {}", ackMessageId,
            taskDefinition.getConversationId());

        ehrExtractStatusService.updateEhrExtractStatusAcknowledgement(
            taskDefinition,
            ackMessageId
        );
    }

    @Getter
    @Setter
    @Builder
    public static class SendAckTemplateParameters {
        private final String fromAsid;
        private final String toAsid;
        private final String creationTime;
        private final String requestId;
        private final String typeCode;
        private final String messageId;
        private final String reasonCode;
        private final String reasonMessage;
    }
}
