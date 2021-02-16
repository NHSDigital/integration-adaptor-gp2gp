package uk.nhs.adaptors.gp2gp.sds.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gp2gp.sds")
@Getter
@Setter
public class SdsConfiguration {
    private String url;
    private String apiKey;
}
