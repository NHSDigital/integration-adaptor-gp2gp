package uk.nhs.adaptors.gp2gp.mhs.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@AllArgsConstructor
public class OutboundMessageWithAttachments {
    private String payload;
    private List<Attachment> attachments;

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
}