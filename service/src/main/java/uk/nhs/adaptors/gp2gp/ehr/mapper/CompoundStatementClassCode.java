package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CompoundStatementClassCode {
    BATTERY("BATTERY"),
    CLUSTER("CLUSTER");

    private final String code;
}
