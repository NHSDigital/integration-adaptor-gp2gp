package uk.nhs.adaptors.gp2gp.gpc.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.configuration.WebClientConfiguration;

@Component
@ConfigurationProperties(prefix = "gp2gp.gpc.client")
public class GpcClientConfiguration extends WebClientConfiguration {
}
