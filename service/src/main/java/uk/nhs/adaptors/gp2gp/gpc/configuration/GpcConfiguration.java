package uk.nhs.adaptors.gp2gp.gpc.configuration;

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
    private String documentEndpoint;
    private String enableProxy;
    private String proxy;
    private String proxyPort;
    private String clientCert;
    private String clientKey;
    private String rootCA;
    private String subCA;

    public String getFormattedClientCert() {
        return PemFormatter.format(getClientCert());
    }

    public String getFormattedClientKey() {
        return PemFormatter.format(getClientKey());
    }

    public String getFormattedSubCA() {
        return PemFormatter.format(getSubCA());
    }

    public String getFormattedRootCA() {
        return PemFormatter.format(getRootCA());
    }
}
