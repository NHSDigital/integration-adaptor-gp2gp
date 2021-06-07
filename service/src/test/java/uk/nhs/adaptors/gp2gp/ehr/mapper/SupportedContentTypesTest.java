package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SupportedContentTypesTest {

    private SupportedContentTypes supportedContentTypes;

    @BeforeEach
    public void setUp() {
        supportedContentTypes = new SupportedContentTypes();
    }

    @Test
    public void When_CheckingIfExecutableContentTypeIsSupported_Expect_False() {
        assertThat(supportedContentTypes.isContentTypeSupported("application/x-dosexec")).isFalse();
    }

    @Test
    public void When_CheckingIfNotSupportedContentTypeIsSupported_Expect_False() {
        assertThat(supportedContentTypes.isContentTypeSupported("application/octet-stream")).isFalse();
    }

    @Test
    public void When_CheckingIfSupportedContentTypeIsSupported_Expect_True() {
        assertThat(supportedContentTypes.isContentTypeSupported("text/plain")).isTrue();
    }
}
