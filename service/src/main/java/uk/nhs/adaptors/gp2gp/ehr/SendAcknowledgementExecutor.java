package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.SendAckTemplateParameters;
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

        var sendAckTemplateParams = SendAckTemplateParameters.builder()
            .creationTime(timestampService.now().toString())
            .uuid(randomIdGeneratorService.createNewId())
            .fromAsid(sendAcknowledgementTaskDefinition.getFromAsid())
            .toAsid(sendAcknowledgementTaskDefinition.getToAsid())
            .typeCode(sendAcknowledgementTaskDefinition.getTypeCode())
            .messageId(sendAcknowledgementTaskDefinition.getEhrRequestMessageId())
            .build();
        var acknowledgementRequestBody = TemplateUtils.fillTemplate(ACKNOWLEDGEMENT_TEMPLATE, sendAckTemplateParams);
        var outboundMessage = OutboundMessage.builder().payload(acknowledgementRequestBody).build();
        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);
        var positiveAckMessageId = randomIdGeneratorService.createNewId();

        var request = mhsRequestBuilder.buildSendAcknowledgement(stringRequestBody, sendAcknowledgementTaskDefinition.getFromOdsCode(),
            sendAcknowledgementTaskDefinition.getConversationId(), positiveAckMessageId);

        mhsClient.sendMessageToMHS(request);

        ehrExtractStatusService.updateEhrExtractStatusAcknowledgement(sendAcknowledgementTaskDefinition, positiveAckMessageId);
    }
}
