package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommentType {
    AGGREGATE_COMMENT_SET("AGGREGATE COMMENT SET"),
    LABORATORY_RESULT_COMMENT("LABORATORY RESULT COMMENT(E141)"),
    USER_COMMENT("USER COMMENT");

    private final String code;
}
