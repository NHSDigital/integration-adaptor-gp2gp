package uk.nhs.adaptors.gp2gp.gpc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcTokenBuilder {
    private static final Mustache JWT_HEADER_TEMPLATE = GpcTemplateUtils.loadTemplate("jwt.header.mustache");
    private static final Mustache JWT_PAYLOAD_TEMPLATE = GpcTemplateUtils.loadTemplate("jwt.payload.mustache");
    private static final int EXPIRY_TIME_ADDITION = 300000;
    private static final int MILLISECOND_DIVISION = 1000;

    private final GpcConfiguration gpcConfiguration;

    public String buildToken(String odsFromCode) throws IOException {
        var creationTime = Instant.now().toEpochMilli();
        var expiryTime = creationTime + EXPIRY_TIME_ADDITION;

        var jwtData = JwtPayloadData.builder()
            .targetURI(gpcConfiguration.getUrl())
            .jwtCreationTime(String.valueOf(creationTime / MILLISECOND_DIVISION))
            .jwtExpiryTime(String.valueOf(expiryTime / MILLISECOND_DIVISION))
            .requestingOrganizationODSCode(odsFromCode)
            .build();

        String jwtHeader = GpcTemplateUtils.fillTemplate(JWT_HEADER_TEMPLATE, new JwtHeaderData());
        String jwtPayload = GpcTemplateUtils.fillTemplate(JWT_PAYLOAD_TEMPLATE, jwtData);

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

