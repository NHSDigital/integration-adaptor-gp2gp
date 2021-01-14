package uk.nhs.adaptors.mockmhsservice.common;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InboundMessage {
    private String payload;
}
