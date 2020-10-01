package uk.nhs.adaptors.gp2gp.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gp-connect")
@Getter
@Setter
public class GpConnectConfiguration {
    //TODO: should those values be in cfg or come from request?
    private String url;
    private String odsCode;
    private String endpoint;

    private String sspFrom;
    private String sspTo;
    private String sspInteractionID;

    private int tokenTtl;
}
