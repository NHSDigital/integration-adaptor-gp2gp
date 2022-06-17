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

public class Gp2GpAdaptorSmokeTest {

    private final static String SERVER_DEFAULT_HOST = "http://localhost";
    private final static String SERVER_PORT_ENV_VARIABLE = "GP2GP_SERVER_PORT";
    private final static String SERVER_PORT_DEFAULT_VALUE = "8080";
    private final static String HEALTHCHECK_ENDPOINT = "/healthcheck";

    private static String invalidResponseMessage;

    private static String serverPort;

    @BeforeAll
    public static void setup() {
        Map<String, String> envVars = System.getenv();

        Optional<String> serverPortOptional = Optional.ofNullable(envVars.get(SERVER_PORT_ENV_VARIABLE));
        serverPort = serverPortOptional.orElse(SERVER_PORT_DEFAULT_VALUE);

        invalidResponseMessage = "Invalid response from GP2GP adaptor at " + SERVER_DEFAULT_HOST + ":" + serverPort;
    }

    @Test
    public void expect_Gp2gpAdaptorIsAvailable() {

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
            fail("Unable to connect to GP2GP Adaptor at " + SERVER_DEFAULT_HOST + ":" + serverPort);
        }

        assertThat(responseBody.isPresent())
            .as(invalidResponseMessage)
            .isTrue();

        if (responseBody.get().contains("DOWN")) {
            fail("The adaptor is running but has a healthcheck status of DOWN. Ensure the other smoke tests pass or skip." );
        }

        assertThat(responseBody.get())
            .as(invalidResponseMessage)
            .contains("UP");
    }
}
