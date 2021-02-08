package uk.nhs.adaptors.gp2gp.common.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
@ConfigurationProperties(prefix = "gp2gp")
@Getter
@Setter
@Slf4j
public class AppConfiguration {
    private static final String S3_PREFIX = "s3";

    private String trustStoreUrl;
    private String trustStorePassword;

    @Bean
    @SuppressWarnings("unused")
    public AmazonS3 getS3Client() {
        if (StringUtils.isNotBlank(trustStoreUrl) && trustStoreUrl.startsWith(S3_PREFIX)) {
            return AmazonS3ClientBuilder.standard()
                .build();
        }

        return null;
    }
}

