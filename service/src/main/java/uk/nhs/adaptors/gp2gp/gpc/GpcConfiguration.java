package uk.nhs.adaptors.gp2gp.gpc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gp-connect")
@Getter
@Setter
public class GpcConfiguration {
    //TODO: should those values be in cfg or come from request?
    private String url;
    private String odsCode;
    private String endpoint;

    private String sspFrom;
    private String sspTo;
    private String sspInteractionID;

    private int tokenTtl;
}