package uk.nhs.adaptors.gp2gp.mhs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Identifier {
    private String system;
    private String value;
}
