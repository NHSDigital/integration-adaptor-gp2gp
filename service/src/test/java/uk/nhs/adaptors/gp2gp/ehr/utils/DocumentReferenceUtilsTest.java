package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import java.util.AbstractMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class DocumentReferenceUtilsTest {
    private static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
    private static final String NARRATIVE_STATEMENT_ID = "3b24b89b-fd14-49f9-ba12-3b4212b60080";
    private static final String FILE_MISSING_ATTACHMENT_TITLE = "Reason why file is missing";
    private static final Map<String, String> SUPPORTED_MIME_TYPES;

    static {
        SUPPORTED_MIME_TYPES = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("text/plain", ".txt"),
            new AbstractMap.SimpleEntry<>("text/html", ".html"),
            new AbstractMap.SimpleEntry<>("application/pdf", ".pdf"),
            new AbstractMap.SimpleEntry<>("text/xml", ".xml"),
            new AbstractMap.SimpleEntry<>("application/xml", ".xml"),
            new AbstractMap.SimpleEntry<>("text/rtf", ".rtf"),
            new AbstractMap.SimpleEntry<>("audio/mpeg", ".mp3"),
            new AbstractMap.SimpleEntry<>("image/png", ".png"),
            new AbstractMap.SimpleEntry<>("image/gif", ".gif"),
            new AbstractMap.SimpleEntry<>("image/jpeg", ".jpg"),
            new AbstractMap.SimpleEntry<>("image/tiff", ".tiff"),
            new AbstractMap.SimpleEntry<>("video/mpeg", ".mpeg"),
            new AbstractMap.SimpleEntry<>("application/msword", ".doc"),
            new AbstractMap.SimpleEntry<>("application/octet-stream", ".bin"),
            new AbstractMap.SimpleEntry<>("audio/basic", ".au"),
            new AbstractMap.SimpleEntry<>("audio/x-au", ".au")
        );
    }

    @Test
    void When_SupportedMimeTypeIsProvided_Expect_CorrectFileExtensionIsGenerated() {
        assertAll(SUPPORTED_MIME_TYPES.entrySet().stream()
            .map(entry -> (Executable) () -> {
                var fileName = DocumentReferenceUtils.buildAttachmentFileName(
                    NARRATIVE_STATEMENT_ID,
                    new Attachment().setContentType(entry.getKey()));

                assertThat(fileName).endsWith(entry.getValue());
            })
            .toArray(Executable[]::new));
    }

    @Test
    void When_ExtractingContentType_Expect_AttachmentContentTypeIsReturned() {
        var attachment = new Attachment().setContentType(TEXT_PLAIN_CONTENT_TYPE);
        assertThat(DocumentReferenceUtils.extractContentType(attachment))
            .isEqualTo(TEXT_PLAIN_CONTENT_TYPE);
    }

    @Test
    void When_ExtractingMissingContentType_Expect_Exception() {
        var attachment = new Attachment();
        assertThatThrownBy(() -> DocumentReferenceUtils.extractContentType(attachment))
            .isInstanceOf(EhrMapperException.class)
            .hasMessage("documentReference.content[0].attachment is missing contentType");
    }

    @Test
    void When_BuildingFileNameIfTitleIsPresent_Expect_MissingAttachmentFileNameIsGenerated() {
        var attachment = new Attachment()
            .setContentType(TEXT_PLAIN_CONTENT_TYPE)
            .setTitle(FILE_MISSING_ATTACHMENT_TITLE);

        assertThat(DocumentReferenceUtils.buildAttachmentFileName(NARRATIVE_STATEMENT_ID, attachment))
            .isEqualTo("AbsentAttachment3b24b89b-fd14-49f9-ba12-3b4212b60080.txt");
    }

    @Test
    void When_BuildingFileNameIfTitleIsAbsent_Expect_FileNameIsGenerated() {
        var attachment = new Attachment()
            .setContentType(TEXT_PLAIN_CONTENT_TYPE);

        assertThat(DocumentReferenceUtils.buildAttachmentFileName(NARRATIVE_STATEMENT_ID, attachment))
            .isEqualTo("3b24b89b-fd14-49f9-ba12-3b4212b60080_3b24b89b-fd14-49f9-ba12-3b4212b60080.txt");
    }
}
