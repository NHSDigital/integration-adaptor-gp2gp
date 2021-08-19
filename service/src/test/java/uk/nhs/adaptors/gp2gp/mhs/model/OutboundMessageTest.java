package uk.nhs.adaptors.gp2gp.mhs.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundMessageTest {

    @Test
    void When_BuildingAttachmentDescriptionUsingMandatoryParameters_Expect_ProperDescriptionIsCreated() {
        var result = OutboundMessage.buildAttachmentDescription(
            "some_file_name",
            "some_content_type",
            false,
            false,
            false,
            null,
            null);

        assertThat(result).isEqualTo("\n" +
            "                Filename=some_file_name\n" +
            "                ContentType=some_content_type\n" +
            "                Compressed=No\n" +
            "                LargeAttachment=No\n" +
            "                OriginalBase64=No\n" +
            "            ");
    }

    @Test
    void When_BuildingAttachmentDescriptionUsingAllParameters_Expect_ProperDescriptionIsCreated() {
        var result = OutboundMessage.buildAttachmentDescription(
            "some_other_file_name",
            "some_other_content_type",
            true,
            true,
            true,
            123,
            "some_other_domain_data");

        assertThat(result).isEqualTo("\n" +
            "                Filename=some_other_file_name\n" +
            "                ContentType=some_other_content_type\n" +
            "                Compressed=Yes\n" +
            "                LargeAttachment=Yes\n" +
            "                OriginalBase64=Yes\n" +
            "                Length=123\n" +
            "                DomainData=some_other_domain_data\n" +
            "            ");
    }
}
