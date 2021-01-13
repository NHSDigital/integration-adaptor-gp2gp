package uk.nhs.adaptors.mockmhsservice.message;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InboundMessage {
    private String payload;
}
