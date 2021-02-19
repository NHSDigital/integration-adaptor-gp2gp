package uk.nhs.adaptors.gp2gp.gpc.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;

@ExtendWith(MockitoExtension.class)
public class GpcTokenBuilderTest {
    private static final int EXPIRY_TIME_ADDITION = 300000;

    @Mock
    private GpcConfiguration gpcConfiguration;

    @Mock
    private TimestampService timestampService;

    @InjectMocks
    private GpcTokenBuilder gpcTokenBuilder;

    @Test
    public void When_gpcJwtTokenIsCreated_Then_iatAndExpAreIntegerSeconds() {
        Instant timestamp = Instant.ofEpochSecond(1613734770L);
        when(timestampService.now()).thenReturn(timestamp);
        when(gpcConfiguration.getUrl()).thenReturn("http://aud");
        String token = gpcTokenBuilder.buildToken("ODS-CODE");
        String[] tokenParts = token.split("\\.");
        String payloadBase64 = tokenParts[1];
        String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);

        String expectedExp = String.format("\"exp\": %d", timestamp.getEpochSecond() + EXPIRY_TIME_ADDITION);
        String expectedIat = String.format("\"iat\": %d", timestamp.getEpochSecond());
        assertThat(payloadJson).contains(expectedExp, expectedIat);
    }

}
