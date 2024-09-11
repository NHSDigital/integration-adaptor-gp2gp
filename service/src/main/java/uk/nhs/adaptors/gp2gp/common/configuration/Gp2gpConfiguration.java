package uk.nhs.adaptors.gp2gp.common.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.annotation.EnableScheduling;

@Getter
@Setter
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "gp2gp")
public class Gp2gpConfiguration {
    private int largeAttachmentThreshold;
    private int largeEhrExtractThreshold;
}
