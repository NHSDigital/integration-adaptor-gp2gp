package uk.nhs.adaptors.gp2gp.mhs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class OutboundMessage {
    private static final int ATTACHMENT_DESCRIPTION_INDENTATION_12 = 12;
    private static final int ATTACHMENT_DESCRIPTION_INDENTATION_16 = 16;
    private static final String LENGTH_PLACEHOLDER = "LENGTH_PLACEHOLDER_ID=";

    private String payload;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Attachment> attachments;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("external_attachments")
    private List<ExternalAttachment> externalAttachments;

    @Getter
    @Setter
    @Jacksonized
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class Attachment {
        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("is_base64")
        private Boolean isBase64;
        private String description;
        private String payload;
    }

    @Getter
    @Setter
    @Jacksonized
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class ExternalAttachment {
        @JsonIgnore
        @JsonProperty("document_id")
        private String documentId;
        @JsonProperty("message_id")
        private String messageId;
        @JsonIgnore
        private String filename;
        private String description;
        @JsonIgnore
        private String title;
        @JsonIgnore
        private String url;
    }

    private static String booleanToYesNo(boolean value) {
        if (value) {
            return "Yes";
        } else {
            return "No";
        }
    }

    @Builder
    public static class AttachmentDescription {
        private final @NonNull String fileName;
        private final @NonNull String contentType;
        private final boolean compressed;
        private final boolean largeAttachment;
        private final boolean originalBase64;
        private final Integer length;
        private final String domainData;
        private final String documentId;

        @Override
        public String toString() {
            var descriptionElements = Stream.of(
                "Filename=\"" + fileName + "\"",
                "ContentType=" + contentType,
                "Compressed=" + booleanToYesNo(compressed),
                "LargeAttachment=" + booleanToYesNo(largeAttachment),
                "OriginalBase64=" + booleanToYesNo(originalBase64),
                Optional.ofNullable(length).map(value -> "Length=" + value).orElse(
                        Optional.ofNullable(documentId)
                        .map(docId -> "Length=${" + LENGTH_PLACEHOLDER + docId + "}")
                        .orElse(StringUtils.EMPTY)),
                Optional.ofNullable(domainData).map(value -> "DomainData=" + value).orElse(StringUtils.EMPTY));
            // all this below to pretty indent on MHS side
            var descriptionWithIndentation = descriptionElements
                .filter(StringUtils::isNotBlank)
                .map(value -> " ".repeat(ATTACHMENT_DESCRIPTION_INDENTATION_16) + value)
                .collect(Collectors.joining("\n"));

            return String.format("%n%s%n%s", descriptionWithIndentation, " ".repeat(ATTACHMENT_DESCRIPTION_INDENTATION_12));
        }
    }
}