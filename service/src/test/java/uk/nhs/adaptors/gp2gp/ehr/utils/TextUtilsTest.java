package uk.nhs.adaptors.gp2gp.ehr.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class TextUtilsTest {
    @Test
    void When_JoiningElementsWithNewLine_Expect_CorrectStringReturned() {
        assertThat(TextUtils.newLine("first line", "second line", "third line"))
            .isEqualTo("first line\nsecond line\nthird line");
    }

    @Test
    void When_JoiningElementsWithSpace_Expect_CorrectStringReturned() {
        assertThat(TextUtils.withSpace("first line", "second line", "third line"))
            .isEqualTo("first line second line third line");
    }

    @Test
    void When_JoiningElementsWithSpace_Expect_EmptyElementNotIgnored() {
        assertThat(TextUtils.withSpace("first line", "", "second line", "", "third line"))
            .isEqualTo("first line  second line  third line");
    }

    @Test
    void When_JoiningElementsWithSpace_Expect_EmptyElementIgnored() {
        assertThat(TextUtils.withSpace(List.of("first line", "", "second line", "", "third line")))
            .isEqualTo("first line second line third line");
    }

    @Test
    void When_JoiningElementsWithSpace_Expect_EmptyStringReturned() {
        assertThat(TextUtils.withSpace(Collections.emptyList()))
            .isEqualTo(StringUtils.EMPTY);
    }
}
