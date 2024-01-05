package uk.nhs.adaptors.gp2gp.mhs.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.common.configuration.WebClientConfiguration;

@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "gp2gp.mhs.client")
public class MhsClientConfiguration extends WebClientConfiguration {
}
