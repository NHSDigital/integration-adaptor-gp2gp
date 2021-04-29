package uk.nhs.adaptors.gp2gp.mhs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboundMessage {
    private String payload;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Attachment> attachments;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ExternalAttachment> externalAttachments;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Attachment {
        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("is_base64")
        private String isBase64;
        private String description;
        private String payload;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class ExternalAttachment {
        @JsonProperty("reference_id")
        private String referenceId;
        @JsonProperty("href_id")
        private String hrefId;
        private String filename;
        @JsonProperty("content_type")
        private String contentType;
        private boolean compressed;
        @JsonProperty("large_attachment")
        private boolean largeAttachment;
        @JsonProperty("original_base64")
        private boolean originalBase64;
        private int length;
        @JsonProperty("reference_id")
        private String domainData;
    }
}