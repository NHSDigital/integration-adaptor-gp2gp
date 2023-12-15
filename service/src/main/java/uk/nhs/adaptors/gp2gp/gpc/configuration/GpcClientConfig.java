package uk.nhs.adaptors.gp2gp.gpc.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.configuration.WebClientConfiguration;

@Component
@ConfigurationProperties(prefix = "gpc.client")
public class GpcClientConfig extends WebClientConfiguration {
}
