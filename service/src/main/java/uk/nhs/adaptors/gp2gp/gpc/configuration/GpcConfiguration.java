package uk.nhs.adaptors.gp2gp.gpc.configuration;

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
    private String migrateStructuredEndpoint;
    private int maxRequestSize;
    private String requestingPractitionerSDSUserId;
    private String requestingPractitionerSDSRoleProfileId;
    private String requestingPractitionerFamilyName;
    private String requestingPractitionerGivenName;
}
