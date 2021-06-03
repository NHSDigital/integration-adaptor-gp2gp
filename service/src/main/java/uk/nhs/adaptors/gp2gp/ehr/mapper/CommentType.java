package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommentType {
    AGGREGATE_COMMENT_SET("AGGREGATE COMMENT SET"),
    COMPLEX_REFERENCE_RANGE("COMPLEX REFERENCE RANGE(E330)"),
    LABORATORY_RESULT_DETAIL("LABORATORY RESULT DETAIL(E136)"),
    LABORATORY_RESULT_COMMENT("LABORATORY RESULT COMMENT(E141)"),
    LAB_SPECIMEN_COMMENT("LAB SPECIMEN COMMENT(E271)"),
    USER_COMMENT("USER COMMENT");

    private final String code;
}
