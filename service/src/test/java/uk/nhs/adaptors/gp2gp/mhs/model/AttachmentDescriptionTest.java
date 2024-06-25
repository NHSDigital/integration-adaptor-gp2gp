package uk.nhs.adaptors.gp2gp.mhs.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentDescriptionTest {

    private static final int LENGTH = 123;

    @Test
    void When_BuildingAttachmentDescriptionUsingMandatoryParameters_Expect_DescriptionIsCreated() {
        var attachmentDescription = getAttachmentDescriptionBuilder()
            .compressed(false)
            .largeAttachment(false)
            .originalBase64(false)
            .length(null)
            .domainData(null)
            .build();

        assertThat(attachmentDescription.toString()).isEqualTo(
            "Filename=\"this_is_a_filename\" "
            + "ContentType=this_is_a_content_type "
            + "Compressed=No "
            + "LargeAttachment=No "
            + "OriginalBase64=No");
    }

    @Test
    void When_BuildingAttachmentDescriptionUsingAllParameters_Expect_DescriptionIsCreated() {
        var attachmentDescription = getAttachmentDescriptionBuilder().build();

        assertThat(attachmentDescription.toString()).isEqualTo(
            "Filename=\"this_is_a_filename\" "
            + "ContentType=this_is_a_content_type "
            + "Compressed=Yes LargeAttachment=Yes "
            + "OriginalBase64=Yes Length=123 "
            + "DomainData=\"some_other_domain_data\"");
    }

    @Test
    void When_BuildingAttachmentDescriptionWithDocumentIdAndWithoutLength_Expect_PlaceholderIsGenerated() {
        var attachmentDescription = getAttachmentDescriptionBuilderWithDocumentId()
                .length(null)
                .build()
                .toString();

        assertThat(attachmentDescription).contains("Length=${LENGTH_PLACEHOLDER_ID=some_document_id}");
    }

    @Test
    void When_BuildingAttachmentDescriptionWithDocumentIdAndWithoutFilename_Expect_PlaceholderIsGenerated() {
        var attachmentDescription = getAttachmentDescriptionBuilderWithDocumentId()
                .fileName(null)
                .build();

        assertThat(attachmentDescription.toString()).contains("Filename=\"${FILENAME_PLACEHOLDER_ID=some_document_id}\"");
    }

    @Test
    void When_BuildingAttachmentDescriptionWithDocumentIdAndWithoutContentType_Expect_PlaceholderIsGenerated() {
        var attachmentDescription = getAttachmentDescriptionBuilderWithDocumentId()
                .contentType(null)
                .build();

        assertThat(attachmentDescription.toString()).contains("ContentType=${CONTENT_TYPE_PLACEHOLDER_ID=some_document_id}");
    }

    private static OutboundMessage.AttachmentDescription.AttachmentDescriptionBuilder getAttachmentDescriptionBuilderWithDocumentId() {
        return getAttachmentDescriptionBuilder().documentId("some_document_id");
    }

    private static OutboundMessage.AttachmentDescription.AttachmentDescriptionBuilder getAttachmentDescriptionBuilder() {
        return OutboundMessage.AttachmentDescription.builder()
                .fileName("this_is_a_filename")
                .contentType("this_is_a_content_type")
                .compressed(true)
                .largeAttachment(true)
                .originalBase64(true)
                .length(LENGTH)
                .domainData("some_other_domain_data");
    }
}
