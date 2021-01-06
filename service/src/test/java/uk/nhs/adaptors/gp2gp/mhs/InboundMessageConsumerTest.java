package uk.nhs.adaptors.gp2gp.mhs;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jms.Message;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class InboundMessageConsumerTest {
    @Mock
    private InboundMessageHandler inboundMessageHandler;
    @Mock
    private Message mockMessage;
    @InjectMocks
    private InboundMessageConsumer inboundMessageConsumer;

    @Test
    @SneakyThrows
    public void When_receivedMessageIsHandled_Then_messageIsAcknowledged() {
        inboundMessageConsumer.receive(mockMessage);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_messageHandlerThrowsException_Then_messageIsNotAcknowledged() {
        doThrow(new RuntimeException()).when(inboundMessageHandler).handle(mockMessage);

        inboundMessageConsumer.receive(mockMessage);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage, times(0)).acknowledge();
    }

}
