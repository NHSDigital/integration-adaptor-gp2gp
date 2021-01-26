package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.common.utils.PemFormatter;

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


    public String getClientCert() {
        return PemFormatter.format(clientCert);
    }

    public String getClientKey() {
        return PemFormatter.format(clientKey);
    }

    public String getSubCA() {
        return PemFormatter.format(subCA);
    }

    public String getRootCA() {
        return PemFormatter.format(rootCA);
    }
}
