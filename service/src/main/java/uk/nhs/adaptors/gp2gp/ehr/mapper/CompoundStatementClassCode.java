package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CompoundStatementClassCode {
    BATTERY("BATTERY"),
    CLUSTER("CLUSTER"),
    TOPIC("TOPIC"),
    CATEGORY("CATEGORY");

    private final String code;
}
