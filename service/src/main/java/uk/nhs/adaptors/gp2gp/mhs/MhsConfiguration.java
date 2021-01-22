package uk.nhs.adaptors.gp2gp.mhs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "gp2gp.mhs")
@Getter
@Setter
public class MhsConfiguration {
    private String url;
}
