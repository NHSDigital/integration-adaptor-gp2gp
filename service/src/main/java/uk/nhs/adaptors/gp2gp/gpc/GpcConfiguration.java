package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "gp-connect")
@Getter
@Setter
public class GpcConfiguration {
    private String url;
    private String structuredEndpoint;
    private String documentEndpoint;
    private String host;
}
