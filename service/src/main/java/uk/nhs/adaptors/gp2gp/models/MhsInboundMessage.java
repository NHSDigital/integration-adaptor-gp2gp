package uk.nhs.adaptors.gp2gp.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MhsInboundMessage {
    private String ebXML;
    private String payload;
}
