package uk.nhs.adaptors.gp2gp.common.enums;

import java.util.Optional;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskEnums {
    DOCUMENT_TASK("DOCUMENT_TASK", "DocumentTask"),
    STRUCTURE_TASK("STRUCTURE_TASK", "StructuredTask");

    private final String code;
    private final String value;

    public static Optional<TaskEnums> fromCode(String code) {
        return Stream.of(values())
            .filter(am -> code.toUpperCase().equals(am.code))
            .findFirst();
    }
}
