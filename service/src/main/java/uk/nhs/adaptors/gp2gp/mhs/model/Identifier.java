package uk.nhs.adaptors.gp2gp.mhs.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Identifier {
    private String system;
    private String value;
}
