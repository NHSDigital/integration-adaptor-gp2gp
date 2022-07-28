package uk.nhs.adaptors.gp2gp.ehr.request;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EhrReceivedAcknowledgement;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EhrReceivedAcknowledgement.EhrReceivedAcknowledgementBuilder;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrExtractAckHandler {
    private static final String ACK_TYPE_CODE_XPATH = "//MCCI_IN010000UK13/acknowledgement/@typeCode";
    private static final String ACK_DETAILS_XPATH = "//MCCI_IN010000UK13/acknowledgement/acknowledgementDetail/code";
    private static final String MESSAGE_REF_XPATH = "//MCCI_IN010000UK13/acknowledgement/messageRef/id/@root";
    private static final String MESSAGE_ID_ROOT_XPATH = "//MCCI_IN010000UK13/id/@root";
    private static final String ERROR_CODE_XPATH = "//justifyingDetectedIssueEvent/code";
    private static final String ACK_OK_CODE = "AA";
    private static final String ACK_BUSINESS_ERROR_CODE = "AE";
    private static final String ACK_REJECTED_CODE = "AR";
    private static final String CODE_ATTRIBUTE = "code";
    private static final String DISPLAY_ATTRIBUTE = "displayName";

    private final XPathService xPathService;
    private final EhrExtractStatusService ehrExtractStatusService;

    @SneakyThrows
    public void handle(String conversationId, Document document) {
        String ackTypeCode = xPathService.getNodeValue(document, ACK_TYPE_CODE_XPATH);
        Instant now = Instant.now();
        String messageRef = xPathService.getNodeValue(document, MESSAGE_REF_XPATH);
        String rootId = xPathService.getNodeValue(document, MESSAGE_ID_ROOT_XPATH);
        EhrReceivedAcknowledgementBuilder ackBuilder = EhrReceivedAcknowledgement.builder()
            .messageRef(messageRef)
            .rootId(rootId)
            .received(now);

        String ehrExtractMessageRef = ehrExtractStatusService
            .fetchEhrExtractMessageId(conversationId)
            .orElseThrow(() -> new EhrExtractException("Unable to fetch EHR Extract Message ID for conversation"));

        LOGGER.debug("******* EHR Extract Message Ref = [{}]", ehrExtractMessageRef);

        if (ACK_OK_CODE.equals(ackTypeCode)) {
            LOGGER.info("Application Acknowledgement Accept ({}) received, messageRef: {}", ackTypeCode, messageRef);

            if (messageRef.equals(ehrExtractMessageRef)) {
                LOGGER.info("Ehr Extract acknowledged: closing conversation {}", conversationId);
                ackBuilder.conversationClosed(now);
                ehrExtractStatusService.updateEhrExtractStatusAck(conversationId, ackBuilder.build());
            }

            return;
        } else if (ACK_BUSINESS_ERROR_CODE.equals(ackTypeCode)) {
            LOGGER.info("Application Acknowledgement Error ({}) received, messageRef: {}", ackTypeCode, messageRef);
            ackBuilder.errors(extractErrorCodes(document, ERROR_CODE_XPATH));
        } else if (ACK_REJECTED_CODE.equals(ackTypeCode)) {
            LOGGER.info("Application Acknowledgement Reject ({}) received, messageRef: {}", ackTypeCode, messageRef);
            ackBuilder.errors(extractErrorCodes(document, ACK_DETAILS_XPATH));
        } else {
            throw new InvalidInboundMessageException(String.format("Unsupported %s: %s", ACK_TYPE_CODE_XPATH, ackTypeCode));
        }

        ehrExtractStatusService.updateEhrExtractStatusAck(conversationId, ackBuilder.build());
    }

    private List<ErrorDetails> extractErrorCodes(Document document, String xPath) {
        NodeList errors = xPathService.getNodes(document, xPath);
        return IntStream.range(0, errors.getLength())
            .mapToObj(errors::item)
            .map(error -> ErrorDetails.builder()
                .code(error.getAttributes().getNamedItem(CODE_ATTRIBUTE).getNodeValue())
                .display(error.getAttributes().getNamedItem(DISPLAY_ATTRIBUTE).getNodeValue())
                .build())
            .collect(Collectors.toList());
    }
}
