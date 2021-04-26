package uk.nhs.adaptors.gp2gp.ehr;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EhrReceivedAcknowledgement;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.adaptors.gp2gp.common.ResourceReader.asString;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrAcknowledgementTest {
    private static final String ROOT_ID = "75049C80-5271-11EA-9384-E83935108FD5";
    private static final String MESSAGE_REF = "BA229526-ADC6-4F1D-970E-CD9E78A6830E";
    private static final String BUSINESS_ERROR_CODE = "99";
    private static final String BUSINESS_ERROR_DISPLAY = "Unexpected condition";
    private static final String REJECTED_ERROR_CODE = "18";
    private static final String REJECTED_ERROR_DISPLAY = "Request message not well-formed or not able to be processed";

    @Autowired
    private EhrExtractRequestHandler ehrExtractRequestHandler;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private XPathService xPathService;
    @Value("classpath:inbound/acknowledgement/MCCI_IN010000UK13_body_AA.xml")
    private Resource positiveAck;
    @Value("classpath:inbound/acknowledgement/MCCI_IN010000UK13_body_AR.xml")
    private Resource rejectedAck;
    @Value("classpath:inbound/acknowledgement/MCCI_IN010000UK13_body_AE.xml")
    private Resource businessErrorAck;

    @SneakyThrows
    @Test
    public void When_EhrReceivedAckAA_Expect_DbUpdatedAndNoErrors() {
        Optional<EhrExtractStatus> ehrExtract = setUpAndHandleAck(positiveAck);

        assertThat(ehrExtract).isPresent()
            .map(EhrExtractStatus::getEhrReceivedAcknowledgement)
            .hasValueSatisfying(ack -> assertAll(
                () -> assertThat(ack.getReceived()).isEqualTo(ack.getConversationClosed()),
                () -> assertThat(ack.getMessageRef()).isEqualTo(MESSAGE_REF),
                () -> assertThat(ack.getRootId()).isEqualTo(ROOT_ID),
                () -> assertThat(ack.getErrors()).isNull()
            ));
    }

    @SneakyThrows
    @Test
    public void When_EhrReceivedNackAE_Expect_DbUpdatedWithErrors() {
        Optional<EhrExtractStatus> ehrExtract = setUpAndHandleAck(businessErrorAck);

        EhrReceivedAcknowledgement ack = ehrExtract.get().getEhrReceivedAcknowledgement();
        assertThat(ack.getReceived()).isNotNull();
        assertThat(ack.getConversationClosed()).isNull();
        assertThat(ack.getMessageRef()).isEqualTo(MESSAGE_REF);
        assertThat(ack.getRootId()).isEqualTo(ROOT_ID);
        assertThat(ack.getErrors()).isNotEmpty();
        EhrReceivedAcknowledgement.ErrorDetails errorDetails = ack.getErrors().get(0);
        assertThat(errorDetails.getCode()).isEqualTo(BUSINESS_ERROR_CODE);
        assertThat(errorDetails.getDisplay()).isEqualTo(BUSINESS_ERROR_DISPLAY);
    }

    @SneakyThrows
    @Test
    public void When_EhrReceivedNackAR_Expect_DbUpdatedWithErrors() {
        Optional<EhrExtractStatus> ehrExtract = setUpAndHandleAck(rejectedAck);

        EhrReceivedAcknowledgement ack = ehrExtract.get().getEhrReceivedAcknowledgement();
        assertThat(ack.getReceived()).isNotNull();
        assertThat(ack.getConversationClosed()).isNull();
        assertThat(ack.getMessageRef()).isEqualTo(MESSAGE_REF);
        assertThat(ack.getRootId()).isEqualTo(ROOT_ID);
        assertThat(ack.getErrors()).isNotEmpty();
        EhrReceivedAcknowledgement.ErrorDetails errorDetails = ack.getErrors().get(0);
        assertThat(errorDetails.getCode()).isEqualTo(REJECTED_ERROR_CODE);
        assertThat(errorDetails.getDisplay()).isEqualTo(REJECTED_ERROR_DISPLAY);
    }

    private Optional<EhrExtractStatus> setUpAndHandleAck(Resource businessErrorAck) throws SAXException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        ehrExtractRequestHandler.handleAcknowledgement(ehrExtractStatus.getConversationId(),
                xPathService.parseDocumentFromXml(asString(businessErrorAck)));

        return ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId());
    }
}
