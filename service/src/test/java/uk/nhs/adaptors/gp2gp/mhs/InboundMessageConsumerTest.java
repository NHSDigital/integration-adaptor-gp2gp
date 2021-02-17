package uk.nhs.adaptors.gp2gp.mhs;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

@ExtendWith(MockitoExtension.class)
public class InboundMessageConsumerTest {
    @Mock
    private InboundMessageHandler inboundMessageHandler;
    @Mock
    private Message mockMessage;
    @Mock
    private MDCService mdcService;
    @InjectMocks
    private InboundMessageConsumer inboundMessageConsumer;

    @Test
    @SneakyThrows
    public void When_ReceivedMessageIsHandled_Expect_MessageIsAcknowledged() {
        inboundMessageConsumer.receive(mockMessage);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_MessageHandlerThrowsException_Expect_MessageIsNotAcknowledged() {
        doThrow(new RuntimeException()).when(inboundMessageHandler).handle(mockMessage);

        inboundMessageConsumer.receive(mockMessage);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage, times(0)).acknowledge();
    }

}
