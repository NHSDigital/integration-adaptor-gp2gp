import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

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

    private final String SERVER_DEFAULT_HOST = "localhost";
    private final static String SERVER_PORT_NAME = "GP2GP_SERVER_PORT";
    private final static String SERVER_PORT_DEFAULT_VALUE = "8080";
    private final static String HEALTHCHECK_ENDPOINT = "/healthcheack";

    private static String serverPort;

    @BeforeAll
    public static void getEnvVars() {
        Map<String, String> envVars = System.getenv();

        Optional<String> serverPortOptional = Optional.ofNullable(envVars.get(SERVER_PORT_NAME));
        serverPort = serverPortOptional.orElse(SERVER_PORT_DEFAULT_VALUE);
    }

    @Test
    public void gp2gpAdaptorIsAvailable() {

        assertThatNoException()
            .as("Unable to connect to GP2GP Adaptor on " + SERVER_DEFAULT_HOST + ":" + serverPort)
            .isThrownBy(() -> {

                try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(
                        "http://" + SERVER_DEFAULT_HOST + ":" + serverPort + HEALTHCHECK_ENDPOINT
                    );

                    final Optional<String> responseBody = httpClient.execute(httpGet, response -> {
                        HttpEntity entity = response.getEntity();

                        try {
                            return entity != null ? Optional.of(EntityUtils.toString(entity)) : Optional.empty();
                        } catch (ParseException e) {
                            return Optional.empty();
                        }
                    });

                    assertThat(responseBody.isPresent())
                        .as("Unable to connect to GP2GP Adaptor on " + SERVER_DEFAULT_HOST + serverPort)
                        .isTrue();

                    assertThat(responseBody.get())
                        .as("Invalid response from GP2GP Adaptor healthcheck endpoint")
                        .isEqualTo("UP");
                }
            });
    }
}
