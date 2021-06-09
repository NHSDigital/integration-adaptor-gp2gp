package uk.nhs.adaptors.gp2gp.ehr;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.Mustache;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendNegativeAcknowledgementExecutor implements TaskExecutor<SendNegativeAcknowledgementTaskDefinition> {
    private static final Mustache NEGATIVE_ACK_TEMPLATE = TemplateUtils.loadTemplate("outbound_message_negative_acknowledgement.mustache");

    private final ObjectMapper objectMapper;
    private final TimestampService timestampService;
    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private final RandomIdGeneratorService randomIdGeneratorService;

    @Override
    public Class<SendNegativeAcknowledgementTaskDefinition> getTaskType() {
        return SendNegativeAcknowledgementTaskDefinition.class;
    }

    @Override
    public void execute(SendNegativeAcknowledgementTaskDefinition taskDefinition) {
        SendNackTemplateParameters sendNackTemplateParameters = SendNackTemplateParameters.builder()
            .requestId(randomIdGeneratorService.createNewId())
            .creationTime(DateFormatUtil.toHl7Format(timestampService.now()))
            .messageId(taskDefinition.getEhrRequestMessageId())
            .fromAsid(taskDefinition.getFromAsid())
            .toAsid(taskDefinition.getToAsid())
            .reasonCode(taskDefinition.getReasonCode())
            .reasonMessage(taskDefinition.getReasonMessage())
            .build();

        String messageBody = buildNackMessage(sendNackTemplateParameters);
        sendNackToMHS(
            messageBody,
            sendNackTemplateParameters.getFromAsid(),
            taskDefinition.getConversationId(),
            sendNackTemplateParameters.getRequestId()
        );
    }

    private String buildNackMessage(SendNackTemplateParameters sendNackTemplateParameters) {
        var negativeAcknowledgementRequestBody = TemplateUtils.fillTemplate(NEGATIVE_ACK_TEMPLATE, sendNackTemplateParameters);
        var outboundMessage = OutboundMessage.builder().payload(negativeAcknowledgementRequestBody).build();
        try {
            return objectMapper.writeValueAsString(outboundMessage);
        } catch (JsonProcessingException e) {
            throw new EhrMapperException("Error while building NACK response", e);
        }
    }

    private void sendNackToMHS(String stringRequestBody, String fromAsid, String conversationId, String nackResponseId) {

        LOGGER.info("Sending NACK message to MHS. NACK message Id: {} Conversation id: {}", nackResponseId, conversationId);
        var request = mhsRequestBuilder.buildSendAcknowledgement(
            stringRequestBody,
            fromAsid,
            conversationId,
            nackResponseId
        );

        mhsClient.sendMessageToMHS(request);
    }

    @Setter
    @Builder
    @Getter
    public static class SendNackTemplateParameters {
        private final String requestId;
        private final String messageId;
        private final String creationTime;
        private final String fromAsid;
        private final String toAsid;
        private final String reasonCode;
        private final String reasonMessage;
    }
}
