package uk.nhs.adaptors.gp2gp.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MhsInboundMessage {
    private final String payload;
}
