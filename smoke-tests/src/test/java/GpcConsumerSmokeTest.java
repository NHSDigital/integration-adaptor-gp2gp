import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GpcConsumerSmokeTest {

    private final static String SERVER_DEFAULT_HOST = "http://localhost";
    private final static String HEALTHCHECK_ENDPOINT = "/healthcheck";

    private static final String SERVER_PORT_ENV_VARIABLE = "GPC_CONSUMER_SERVER_PORT";
    private static final String SERVER_PORT_DEFAULT_VALUE = "8090";

    private static String serverPort;
    private static String invalidResponseMessage;

    private static Map<String, String> envVars;

    @BeforeAll
    public static void setup() {
        envVars = System.getenv();

        Optional<String> serverPortOptional = Optional.ofNullable(envVars.get(SERVER_PORT_ENV_VARIABLE));
        serverPort = serverPortOptional.orElse(SERVER_PORT_DEFAULT_VALUE);

        invalidResponseMessage = "Invalid response from GPC Consumer at " + SERVER_DEFAULT_HOST + ":" + serverPort;
    }

    @Test
    public void expect_GpcConsumerIsAvailable() {

        Optional<String> responseBody = Optional.empty();

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(
                    SERVER_DEFAULT_HOST + ":" + serverPort + HEALTHCHECK_ENDPOINT
            );

            responseBody = httpClient.execute(httpGet, response -> {
                HttpEntity entity = response.getEntity();

                try {
                    return entity != null ? Optional.of(EntityUtils.toString(entity)) : Optional.empty();
                } catch (ParseException e) {
                    return Optional.empty();
                }
            });
        } catch (IOException e) {
            fail("Unable to connect to GPC Consumer at " + SERVER_DEFAULT_HOST + ":" + serverPort);
        }

        assertThat(responseBody.isPresent())
                .as(invalidResponseMessage)
                .isTrue();

        assertThat(responseBody.get())
                .as(invalidResponseMessage)
                .contains("UP");
    }
}
