package uk.nhs.adaptors.gp2gp.services;

import com.github.mustachejava.Mustache;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.configurations.GpConnectConfiguration;
import uk.nhs.adaptors.gp2gp.utils.TemplateUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TokenBuilder {
    private static final Mustache JWT_HEADER_TEMPLATE = TemplateUtils.loadTemplate("jwt.header.mustache");
    private static final Mustache JWT_PAYLOAD_TEMPLATE = TemplateUtils.loadTemplate("jwt.payload.mustache");

    private final GpConnectConfiguration gpConnectConfiguration;

    public String buildToken() {

        var creationTime = Instant.now().toEpochMilli();
        var expiryTime = creationTime + gpConnectConfiguration.getTokenTtl(); //300000;

        //TODO: proper token builder
        var jwtData = JwtPayloadData.builder()
            .targetURI(gpConnectConfiguration.getUrl())
            .jwtCreationTime(String.valueOf(creationTime / 1000))
            .jwtExpiryTime(String.valueOf(expiryTime / 1000))
            .requestingOrganizationODSCode(gpConnectConfiguration.getOdsCode())
            .build();

        String jwtHeader = TemplateUtils.fillTemplate(JWT_HEADER_TEMPLATE, new JwtHeaderData());
        String jwtPayload = TemplateUtils.fillTemplate(JWT_PAYLOAD_TEMPLATE, jwtData);

        String encodedJwtHeader = encode(jwtHeader);
        String encodedJwtPayload = encode(jwtPayload);

        return String.format("%s.%s.", encodedJwtHeader, encodedJwtPayload);
    }

    private static String encode(String data) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static final class JwtHeaderData {
        private String alg;
        private String typ;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static final class JwtPayloadData {
        private String targetURI;
        private String jwtExpiryTime;
        private String jwtCreationTime;
        private String requestingOrganizationODSCode;
    }
}
