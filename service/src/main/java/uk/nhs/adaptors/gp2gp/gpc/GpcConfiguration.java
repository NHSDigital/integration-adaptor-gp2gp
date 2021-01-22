package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "gp2gp.gpc")
@Getter
@Setter
public class GpcConfiguration {
    private String url;
    private String structuredEndpoint;
    private String host;
    private String enableTLS;
    private String enableProxy;
    private String proxy;
    private String proxyPort;
    private String clientCert;
    private String clientKey;
    private String rootCA;
    private String subCA;
}
