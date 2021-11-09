package uk.nhs.adaptors.gp2gp.mhs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.w3c.dom.Document;

import java.time.Instant;

@Getter
@AllArgsConstructor
@Builder
public class ParsedInboundMessage {
    private final Document ebXMLDocument;
    private final Document payloadDocument;
    private final String rawPayload;
    private final String conversationId;
    private final String interactionId;
    private final Instant messageTimestamp;
}
