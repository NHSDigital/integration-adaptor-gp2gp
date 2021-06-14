package uk.nhs.adaptors.gp2gp.ehr;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.ResourceReader.asString;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import javax.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageHandler;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class IllogicalMessageComponentTest {
    private static final String EHR_EXTRACT_REQUEST = "RCMR_IN010000UK05";
    private static final String CONTINUE_REQUEST = "COPC_IN000001UK01";
    private static final String ACKNOWLEDGMENT_REQUEST = "MCCI_IN010000UK13";
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";

    @Autowired
    private InboundMessageHandler inboundMessageHandler;
    @Autowired
    private XPathService xPathService2;
    @MockBean
    private ObjectMapper objectMapper;
    @MockBean
    private XPathService xPathService;

    @Mock
    private Message message;
    @Mock
    private InboundMessage inboundMessage;

    @Value("classpath:illogicalmessage/COPC_IN000001UK01_ebxml.txt")
    private Resource continueResponseEbxml;
    @Value("classpath:illogicalmessage/COPC_IN000001UK01_payload.txt")
    private Resource continueResponsePayload;

    //Message not in flight
    @Test
    public void When_ContinueSentAndNoEhrExtractStatusExists_Expect_ErrorThrown() throws JsonProcessingException {
        String continuePayload = asString(continueResponsePayload);
        String continueEbxml = asString(continueResponseEbxml);
        String vasr = null;
        when(objectMapper.readValue(vasr, InboundMessage.class)).thenReturn(inboundMessage);
        when(inboundMessage.getEbXML()).thenReturn(continueEbxml);
        when(inboundMessage.getPayload()).thenReturn(continuePayload);

        //when(xPathService.getNodeValue(vasr, ACTION_PATH)).thenReturn(CONTINUE_REQUEST);
        inboundMessageHandler.handle(message);
    }

    @Test
    public void When_AcknowledgementSentAndNoEhrExtractStatusExists_Expect_ErrorThrown() {

    }

    //outoforder
    @Test
    public void When_ContinueReceivedAndCoreNotSent_Expect_ErrorThrown() {

    }

    @Test
    public void When_ContinueReceivedAndNoDocumentsExist_Expect_ErrorThrown() {

    }

    @Test
    public void When_AcknowledgementReceivedAndAcknowledgmentNotSent_Expect_ErrorThrown() {

    }

    //Duplicates
    @Test
    public void When_ExtractSentTwice_Expect_ErrorThrown() {

    }

    @Test
    public void When_ContinueSentTwice_Expect_ErrorThrown() {

    }

    @Test
    public void When_AcknowledgementSentTwice_Expect_ErrorThrown() {

    }

    //unsupported
    @Test
    public void When_UnsupportedMessageSent_Expect_ErrorThrown() {

    }

    public static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
