package uk.nhs.adaptors.gp2gp.mhs.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentDescriptionTest {

    private static final int LENGTH = 123;

    @Test
    void When_BuildingAttachmentDescriptionUsingMandatoryParameters_Expect_ProperDescriptionIsCreated() {
        var result = OutboundMessage.AttachmentDescription.builder()
            .fileName("some_file_name")
            .contentType("some_content_type")
            .compressed(false)
            .largeAttachment(false)
            .originalBase64(false)
            .build()
            .toString();

        assertThat(result).isEqualTo("\n"
            + "                Filename=some_file_name\n"
            + "                ContentType=some_content_type\n"
            + "                Compressed=No\n"
            + "                LargeAttachment=No\n"
            + "                OriginalBase64=No\n"
            + "            ");
    }

    @Test
    void When_BuildingAttachmentDescriptionUsingAllParameters_Expect_ProperDescriptionIsCreated() {
        var result = OutboundMessage.AttachmentDescription.builder()
            .fileName("some_other_file_name")
            .contentType("some_other_content_type")
            .compressed(true)
            .largeAttachment(true)
            .originalBase64(true)
            .length(LENGTH)
            .domainData("some_other_domain_data")
            .build()
            .toString();

        assertThat(result).isEqualTo("\n"
            + "                Filename=some_other_file_name\n"
            + "                ContentType=some_other_content_type\n"
            + "                Compressed=Yes\n"
            + "                LargeAttachment=Yes\n"
            + "                OriginalBase64=Yes\n"
            + "                Length=123\n"
            + "                DomainData=some_other_domain_data\n"
            + "            ");
    }
}
